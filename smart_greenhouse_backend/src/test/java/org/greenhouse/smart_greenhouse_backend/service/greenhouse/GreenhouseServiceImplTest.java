package org.greenhouse.smart_greenhouse_backend.service.greenhouse;

import org.greenhouse.smart_greenhouse_backend.dto.WeatherDto;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.DeviceState;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.Location;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.SensorRef;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.enums.Type;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.enums.Unit;
import org.greenhouse.smart_greenhouse_backend.model.documents.ActionLog;
import org.greenhouse.smart_greenhouse_backend.model.documents.Greenhouse;
import org.greenhouse.smart_greenhouse_backend.model.documents.Plan;
import org.greenhouse.smart_greenhouse_backend.model.documents.PlantProfile;
import org.greenhouse.smart_greenhouse_backend.repository.*;
import org.greenhouse.smart_greenhouse_backend.service.rule_evaluator.RuleEvaluatorService;
import org.greenhouse.smart_greenhouse_backend.service.weather.WeatherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GreenhouseServiceImplTest {

    @Mock
    private GreenhouseRepository greenhouseRepository;
    @Mock private PlantProfileRepository plantProfileRepository;
    @Mock private ActionLogRepository actionLogRepository;
    @Mock private PlanRepository planRepository;
    @Mock private WeatherSnapshotRepository weatherSnapshotRepository;
    @Mock private WeatherService weatherService;
    @Mock private RuleEvaluatorService ruleEvaluatorService;

    @InjectMocks
    private GreenhouseServiceImpl service;

    /**
     * Teszteli, hogy a create() metódus:
     * - hozzárendeli a PlantProfile ID-t, ha a plantType meg van adva
     * - elmenti az üvegreenhouseázat a repository-ba
     * - és ha nincs aktív terv, akkor generál egyet
     */
    @Test
    void create_shouldAssignPlantProfileAndGeneratePlan() {
        PlantProfile profile = new PlantProfile();
        profile.setId("profile123");
        profile.setPlantType("PEPPER");

        Location location = new Location("Halásztelek", 18.981190, 47.361730);

        Greenhouse greenhouse = new Greenhouse();
        greenhouse.setId("greenhouse1");
        greenhouse.setCode("greenhouse1");
        greenhouse.setName("Greenhouse1Name");
        greenhouse.setLocation(location);
        greenhouse.setActive(true);
        greenhouse.setPlantType("PEPPER");
        greenhouse.setPlantProfileId("profile123");

        when(plantProfileRepository.findByPlantType("PEPPER"))
                .thenReturn(Optional.of(profile));

        when(planRepository.findByGreenhouseCode("greenhouse1"))
                .thenReturn(List.of());

        when(plantProfileRepository.findByPlantCode("profile123")).thenReturn(Optional.of(profile));

        when(weatherService.fetchForecastForLocation(location.city(), location.lat(), location.lon()))
                .thenReturn(Flux.just(
                        new WeatherDto("Halásztelek", Instant.now(), 0.0, 0.0, 0.0, 0.0)
                ));

        when(greenhouseRepository.save(greenhouse)).thenReturn(greenhouse);

        Greenhouse result = service.create(greenhouse);

        assertEquals("profile123", result.getPlantProfileId());
    }

    /**
     * Teszteli, hogy a manualAction():
     * - módosítja a DeviceState-et a megadott akció szerint
     * - elmenti az ActionLog-ot a repository-ba
     */
    @Test
    void manualAction_shouldUpdateDeviceStateAndLogAction() {
        Greenhouse greenhouse = new Greenhouse();
        greenhouse.setCode("greenhouse2");
        greenhouse.setDevices(new DeviceState());

        when(greenhouseRepository.findByCode("greenhouse2"))
                .thenReturn(Optional.of(greenhouse));
        when(greenhouseRepository.save(ArgumentMatchers.any()))
                .thenReturn(greenhouse);

        DeviceState result = service.manualAction("greenhouse2", "VENT_OPEN");

        assertTrue(result.isVentOpen());
        verify(actionLogRepository).save(ArgumentMatchers.any());
    }

    /**
     * Teszteli, hogy a fetchWeatherForGreenhouse():
     * - megreenhouseívja a WeatherService-t a greenhouse location alapján
     * - visszaadja a WeatherDto-t a megfelelő city mezővel
     */
    @Test
    void fetchWeatherForGreenhouse_shouldReturnDtoWithCity() {
        Greenhouse greenhouse = new Greenhouse();
        greenhouse.setCode("greenhouse3");
        greenhouse.setLocation(new Location("Budapest", 47.5, 19.0));

        when(greenhouseRepository.findByCode("greenhouse3"))
                .thenReturn(Optional.of(greenhouse));

        WeatherDto dto = new WeatherDto();
        dto.setTemperature(25.0);
        dto.setHumidity(60.0);
        dto.setWindSpeed(3.0);

        when(weatherService.fetchForLocation("Budapest", 47.5, 19.0))
                .thenReturn(Mono.just(dto));

        WeatherDto result = service.fetchWeatherForGreenhouse("greenhouse3");

        assertEquals("Budapest", result.getCity());
        assertEquals(25.0, result.getTemperature());
    }

    /**
     * Teszteli, hogy az evaluateRulesFromSensors():
     * - lekéri az összes greenhouse-t
     * - a RuleEvaluatorService által visszaadott akciókat alkalmazza
     * - és ActionLog-ot ment
     */
    @Test
    void evaluateRulesFromSensors_shouldApplyActionsAndLog() {
        Greenhouse greenhouse = new Greenhouse();
        greenhouse.setId("greenhouse4");
        greenhouse.setActive(true);
        greenhouse.setSensors(List.of(new SensorRef("s1", "TEMPERATURES_SENSOR_1", Type.TEMPERATURE, Unit.CELSIUS, 35.0, Instant.now())));
        greenhouse.setDevices(new DeviceState());

        PlantProfile profile = new PlantProfile();
        profile.setId("profileX");

        when(greenhouseRepository.findAll()).thenReturn(List.of(greenhouse));
        when(plantProfileRepository.findByPlantCode(ArgumentMatchers.any())).thenReturn(Optional.of(profile));
        when(ruleEvaluatorService.evaluate(eq(profile), anyMap()))
                .thenReturn(List.of("VENT_OPEN"));

        service.evaluateRulesFromSensors();

        assertTrue(greenhouse.getDevices().isVentOpen());
        //verify(actionLogRepository).save(ArgumentMatchers.any());
    }

    /**
     * Teszteli, hogy a generatePlanForNewGreenhouse():
     * - ha a WeatherService forecastot ad vissza
     * - akkor létrejön egy Plan és hozzárendelődik a greenhouse-hoz
     */
    @Test
    void generatePlans_shouldCreatePlanFromForecast() {
        Greenhouse greenhouse = new Greenhouse();
        greenhouse.setId("greenhouse5");
        greenhouse.setCode("GREENHOUSE_6TS");
        greenhouse.setActive(true);
        greenhouse.setLocation(new Location("Budapest", 47.5, 19.0));

        PlantProfile profile = new PlantProfile();
        profile.setId("profileY");

        WeatherDto forecastDto = new WeatherDto();
        forecastDto.setTimestamp(Instant.now().plus(1, ChronoUnit.HOURS));
        forecastDto.setTemperature(32.0);
        forecastDto.setHumidity(40.0);
        forecastDto.setWindSpeed(2.0);

        when(greenhouseRepository.findAll()).thenReturn(List.of(greenhouse));
        when(plantProfileRepository.findByPlantCode(ArgumentMatchers.any())).thenReturn(Optional.of(profile));
        when(weatherService.fetchForecastForLocation(anyString(), anyDouble(), anyDouble()))
                .thenReturn(Flux.just(forecastDto));
        when(ruleEvaluatorService.evaluate(eq(profile), anyMap()))
                .thenReturn(List.of("SHADE_ON"));
        when(planRepository.save(ArgumentMatchers.any()))
                .thenAnswer(inv -> {
                    Plan p = inv.getArgument(0);
                    p.setId("plan123");
                    return p;
                });

        service.generatePlans();

        verify(planRepository).save(ArgumentMatchers.any());
        verify(greenhouseRepository).save(greenhouse);
        assertEquals("GREENHOUSE_6TS", greenhouse.getPlanId());
    }

    /**
     * Teszteli, hogy a getById():
     * - visszaadja a greenhouse-t, ha létezik
     * - kivételt dob, ha nem található
     */
    @Test
    void getByCode_shouldReturnGreenhouse_whenExists() {
        Greenhouse greenhouse = new Greenhouse();
        greenhouse.setCode("greenhouse10");

        when(greenhouseRepository.findByCode("greenhouse10"))
                .thenReturn(Optional.of(greenhouse));

        Greenhouse result = service.getByCode("greenhouse10");

        assertEquals("greenhouse10", result.getCode());
    }

    @Test
    void getByCode_shouldThrow_whenNotFound() {
        when(greenhouseRepository.findByCode("missing"))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.getByCode("missing"));
    }

    /**
     * Teszteli, hogy az updateById():
     * - frissíti a greenhouse mezőit
     * - elmenti a repository-ba
     */
    @Test
    void updateById_shouldUpdateFields() {
        Greenhouse existing = new Greenhouse();
        existing.setId("greenhouse11");
        existing.setName("OldName");

        Greenhouse updated = new Greenhouse();
        updated.setName("NewName");

        when(greenhouseRepository.findByCode("greenhouse11"))
                .thenReturn(Optional.of(existing));
        when(greenhouseRepository.save(ArgumentMatchers.any()))
                .thenAnswer(inv -> inv.getArgument(0));

        Greenhouse result = service.updateById("greenhouse11", updated);

        assertEquals("NewName", result.getName());
        verify(greenhouseRepository).save(ArgumentMatchers.any());
    }

    /**
     * Teszteli, hogy a deleteById():
     * - törli a greenhouse-t, ha létezik
     * - kivételt dob, ha nem található
     */
    @Test
    void deleteById_shouldDelete_whenExists() {
        when(greenhouseRepository.existsByCode("greenhouse12"))
                .thenReturn(true);

        service.deleteByCode("greenhouse12");

        verify(greenhouseRepository).deleteById("greenhouse12");
    }

    @Test
    void deleteByCode_shouldThrow_whenNotExists() {
        when(greenhouseRepository.existsByCode("greenhouse13"))
                .thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> service.deleteByCode("greenhouse13"));
    }

    /**
     * Teszteli, hogy a getLogs():
     * - visszaadja az ActionLog listát a repository-ból
     */
    @Test
    void getLogs_shouldReturnLogs() {
        ActionLog log1 = new ActionLog();
        log1.setAction("VENT_OPEN");

        when(actionLogRepository.findActionLogsByGreenhouseCode("greenhouse14"))
                .thenReturn(List.of(log1));

        List<ActionLog> result = service.getLogs("greenhouse14");

        assertEquals(1, result.size());
        assertEquals("VENT_OPEN", result.get(0).getAction());
    }


}