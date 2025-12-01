package org.greenhouse.smart_greenhouse_backend.service.greenhouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.greenhouse.smart_greenhouse_backend.dto.WeatherDto;
import org.greenhouse.smart_greenhouse_backend.exception.GreenhouseAlreadyExistsException;
import org.greenhouse.smart_greenhouse_backend.exception.GreenhouseNotFoundException;
import org.greenhouse.smart_greenhouse_backend.exception.PlanNotFoundForGreenhouseException;
import org.greenhouse.smart_greenhouse_backend.exception.PlanNotFoundException;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.DeviceState;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.Location;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.PlannedEvent;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.SensorRef;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.enums.Type;
import org.greenhouse.smart_greenhouse_backend.model.documents.*;
import org.greenhouse.smart_greenhouse_backend.repository.*;
import org.greenhouse.smart_greenhouse_backend.service.rule_evaluator.RuleEvaluatorService;
import org.greenhouse.smart_greenhouse_backend.service.weather.WeatherService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GreenhouseServiceImpl implements GreenhouseService {

    private final GreenhouseRepository greenhouseRepository;
    private final PlantProfileRepository plantProfileRepository;
    private final ActionLogRepository actionLogRepository;
    private final PlanRepository planRepository;
    private final WeatherSnapshotRepository weatherSnapshotRepository;

    private final WeatherService weatherService;
    private final RuleEvaluatorService ruleEvaluatorService;

    @Override
    public Greenhouse create(final Greenhouse greenhouse) {
        if(!greenhouseRepository.existsByCode(greenhouse.getCode())) {
            if (greenhouse.getPlantType() != null) {
                PlantProfile profile = plantProfileRepository.findByPlantType(greenhouse.getPlantType())
                        .orElseThrow(() -> new PlanNotFoundException(greenhouse.getPlantType()));
                greenhouse.setPlantProfileId(profile.getId());
            }

            Greenhouse saved = greenhouseRepository.save(greenhouse);

            // ha az adott greenhouse-hoz nincs aktív terv, automatikusan generáljunk egyet
            boolean hasPlan = planRepository.findByGreenhouseCode(saved.getCode()) // inkább code, nem id
                    .stream()
                    .anyMatch(Plan::isActive);

            if (!hasPlan) {
                generatePlanForNewGreenhouse(saved);
            }
            return saved;
        }
        throw new GreenhouseAlreadyExistsException("Az üvegház már lérezik ezzel a kóddal");
    }

    @Override
    public List<Greenhouse> listAll() {
        return greenhouseRepository.findAll();
    }

    @Override
    public Greenhouse getByCode(final String code) {
        return greenhouseRepository.findByCode(code)
                .orElseThrow(() -> new GreenhouseNotFoundException("Greenhouse not found: " + code));
    }

    @Override
    public Greenhouse updateById(final String code, final Greenhouse updated) {
        Greenhouse existing = getByCode(code);
        existing.setName(updated.getName());
        existing.setPlantType(updated.getPlantType());
        existing.setActive(updated.isActive());
        existing.setLocation(updated.getLocation());
        existing.setSensors(updated.getSensors());
        existing.setDevices(updated.getDevices());
        existing.setPlantProfileId(updated.getPlantProfileId());

        return greenhouseRepository.save(existing);
    }

    @Override
    public void deleteByCode(final String code) {
        Greenhouse greenhouse = getByCode(code);
        greenhouseRepository.deleteById(greenhouse.getId());
    }

    @Override
    public Greenhouse addSensorData(final String code, final SensorRef sensor) {
        Greenhouse greenhouse = getByCode(code);

        SensorRef sensorWithTimestamp = sensor.lastSeen() == null
                ? new SensorRef(
                sensor.id(),
                sensor.code(),
                sensor.type(),
                sensor.unit(),
                sensor.lastValue(),
                Instant.now()
        )
                : sensor;

        greenhouse.getSensors().removeIf(sensorRef ->
                sensorRef.id().equals(sensor.id()) || sensorRef.code().equals(sensor.code())
        );

        greenhouse.getSensors().add(sensorWithTimestamp);

        return greenhouseRepository.save(greenhouse);
    }

    @Override
    public DeviceState getState(final String code) {
        return getByCode(code).getDevices();
    }

    @Override
    public DeviceState manualAction(final String id, final String action) {
        Greenhouse greenhouse = getByCode(id);
        DeviceState devices = greenhouse.getDevices();

        switch (action.toUpperCase()) {
            case "IRRIGATION_ON" -> devices.setIrrigationOn(true);
            case "IRRIGATION_OFF" -> devices.setIrrigationOn(false);
            case "VENT_OPEN" -> devices.setVentOpen(true);
            case "VENT_CLOSE" -> devices.setVentOpen(false);
            case "SHADE_ON" -> devices.setShadeOn(true);
            case "SHADE_OFF" -> devices.setShadeOn(false);
            case "LIGHT_ON" -> devices.setLightOn(true);
            case "LIGHT_OFF" -> devices.setLightOn(false);
            case "HUMIDIFIER_ON" -> devices.setHumidifierOn(true);
            case "HUMIDIFIER_OFF" -> devices.setHumidifierOn(false);
            default -> throw new IllegalArgumentException("Unknown action: " + action);
        }

        greenhouseRepository.save(greenhouse);

        // naplózás
        ActionLog log = new ActionLog();
        log.setGreenhouseCode(id);
        log.setTimestamp(Instant.now());
        log.setAction(action);
        log.setReason("manual");
        actionLogRepository.save(log);

        return devices;
    }

    @Override
    public WeatherDto fetchWeatherForGreenhouse(final String code) {
        Greenhouse greenhouse = getByCode(code);
        WeatherDto dto = weatherService.fetchForLocation(
                greenhouse.getLocation().city(),
                greenhouse.getLocation().lat(),
                greenhouse.getLocation().lon()
        ).block();

        if (dto != null) {
            dto.setCity(greenhouse.getLocation().city());
            dto.setTimestamp(Instant.now());
        }
        return dto;
    }

    @Override
    public List<WeatherDto> fetchForecastForGreenhouse(final String id) {
        Greenhouse greenhouse = getByCode(id);

        List<WeatherDto> forecast = weatherService.fetchForecastForLocation(
                greenhouse.getLocation().city(),
                greenhouse.getLocation().lat(),
                greenhouse.getLocation().lon()
        ).collectList().block();

        if (forecast != null) {
            forecast.forEach(dto -> dto.setCity(greenhouse.getLocation().city()));
        }

        return forecast;
    }

    @Override
    public List<ActionLog> getLogs(final String code) {
        return actionLogRepository.findActionLogsByGreenhouseCode(code);
    }

    @Override
    public void evaluateRulesFromSensors() {
        for (Greenhouse greenhouse : greenhouseRepository.findAll()) {
            if (!greenhouse.isActive()) continue;

            PlantProfile profile = plantProfileRepository.findByPlantCode(greenhouse.getCode())
                    .orElse(null);
            if (profile == null) continue;

            Map<Type, Double> values = greenhouse.getSensors().stream()
                    .collect(Collectors.toMap(
                            SensorRef::type,
                            SensorRef::lastValue,
                            (s1, s2) -> s1
                    ));

            var actions = ruleEvaluatorService.evaluate(profile, values);
            applyActions(greenhouse, actions, "rule-engine");
            log.info("evaluateRulesFromSensors lefutott");
        }
    }

    @Override
    public void evaluateRulesFromWeather() {
        List<Greenhouse> greenhouses = greenhouseRepository.findAll();
        if (greenhouses.isEmpty()) {
            log.warn("Nincs greenhouse az adatbázisban, kihagyom a futást");
            return;
        }

        for (Greenhouse greenhouse : greenhouses) {
            if (greenhouse.getPlantProfileId() == null) {
                log.warn("Greenhouse {} nem rendelkezik plantProfileId-vel", greenhouse.getId());
            }
        }

        for (Greenhouse greenhouse : greenhouseRepository.findAll()) {
            if (!greenhouse.isActive()) continue;

            PlantProfile profile = plantProfileRepository.findByPlantCode(greenhouse.getPlantProfileId())
                    .orElse(null);
            if (profile == null) continue;

            WeatherDto weather = weatherService.fetchForLocation(
                    greenhouse.getLocation().city(),
                    greenhouse.getLocation().lat(),
                    greenhouse.getLocation().lon()
            ).block();

            if (weather == null) continue;

            Map<Type, Double> values = new HashMap<>();
            values.put(Type.TEMPERATURE, weather.getTemperature());
            values.put(Type.HUMIDITY_PCT, weather.getHumidity());
            values.put(Type.WIND_SPEED, weather.getWindSpeed());
            if (weather.getPrecipitationMm() != null) {
                values.put(Type.PRECIPITATION_MM, weather.getPrecipitationMm());
            }

            List<String> actions = ruleEvaluatorService.evaluate(profile, values);
            applyActions(greenhouse, actions, "weather-check");
        }
        log.info("evaluateRulesFromWeather lefutott");
    }

    @Override
    public void generatePlans() {
        for (Greenhouse greenhouse : greenhouseRepository.findAll()) {
            if (!greenhouse.isActive()) continue;
            generatePlanForNewGreenhouse(greenhouse);
        }
    }

    private void generatePlanForNewGreenhouse(final Greenhouse greenhouse) {
        if (greenhouse == null || !greenhouse.isActive())
            return;

        PlantProfile profile = plantProfileRepository.findByPlantCode(greenhouse.getPlantProfileId())
                .orElse(null);
        if (profile == null)
            return;

        List<WeatherDto> forecast = weatherService.fetchForecastForLocation(
                greenhouse.getLocation().city(),
                greenhouse.getLocation().lat(),
                greenhouse.getLocation().lon()
        ).collectList().block();

        if (forecast == null || forecast.isEmpty())
            return;

        Plan plan = new Plan();
        plan.setGreenhouseCode(greenhouse.getCode());
        plan.setValidFrom(Instant.now());
        plan.setValidTo(Instant.now().plus(3, ChronoUnit.DAYS));
        plan.setActive(true);

        List<PlannedEvent> events = new ArrayList<>();

        for (WeatherDto weatherDto : forecast) {
            Map<Type, Double> values = new HashMap<>();
            values.put(Type.TEMPERATURE, weatherDto.getTemperature());
            values.put(Type.HUMIDITY_PCT, weatherDto.getHumidity());
            values.put(Type.WIND_SPEED, weatherDto.getWindSpeed());
            if (weatherDto.getPrecipitationMm() != null) {
                values.put(Type.PRECIPITATION_MM, weatherDto.getPrecipitationMm());
            }

            List<String> actions = ruleEvaluatorService.evaluate(profile, values);
            if (!actions.isEmpty()) {
                PlannedEvent plannedEvent = new PlannedEvent(
                        weatherDto.getTimestamp(),
                        weatherDto.getTemperature(),
                        weatherDto.getHumidity(),
                        weatherDto.getWindSpeed(),
                        weatherDto.getPrecipitationMm(),
                        actions,
                        "IRRIGATION",
                        "START",
                        10,
                        "generated from forecast"
                );

                events.add(plannedEvent);
            }
        }

        plan.setEvents(events);
        planRepository.save(plan);

        greenhouse.setPlanId(plan.getId());
        greenhouseRepository.save(greenhouse);
        log.info("Új greenhouse-hoz alap plan létrehozva: {}", plan.getId());
    }

    @Override
    public Plan getLastPlanForGreenhouse(final String code) {
        Greenhouse greenhouse = getByCode(code);

        return planRepository.findByGreenhouseCode(greenhouse.getCode()).stream()
                .filter(Plan::isActive)
                .reduce((first, second) -> second)
                .orElseThrow(() -> new PlanNotFoundForGreenhouseException(greenhouse.getCode()));
    }

    @Override
    public void pollAllGreenhouses() {
        List<Greenhouse> greenhouses = greenhouseRepository.findAll();
        if (greenhouses.isEmpty()) {
            log.warn("Nincs greenhouse az adatbázisban, kihagyom a futást");
            return;
        }

        for (Greenhouse greenhouse : greenhouses) {
            if (greenhouse.getPlantProfileId() == null) {
                log.warn("Greenhouse {} nem rendelkezik plantProfileId-vel", greenhouse.getId());
                continue;
            }
        }

        for (Greenhouse greenhouse : greenhouseRepository.findAll()) {
            WeatherDto weather = fetchWeatherForGreenhouse(greenhouse.getCode());
            if (weather == null) continue;

            WeatherSnapshot snapshot = WeatherSnapshot.builder()
                    .greenhouseCode(greenhouse.getCode())
                    .timestamp(Instant.now())
                    .temperature(weather.getTemperature())
                    .humidity(weather.getHumidity())
                    .windSpeed(weather.getWindSpeed())
                    .precipitationMm(weather.getPrecipitationMm())
                    .build();

            log.info("Actual weatherSnapshot: {}", snapshot);
            weatherSnapshotRepository.save(snapshot);
        }
    }

    /**
     * Akciók alkalmazása egy üvegházban és naplózása.
     */
    private void applyActions(final Greenhouse greenhouse,
                              final List<String> actions,
                              final String reason) {
        DeviceState devices = greenhouse.getDevices();
        for (String action : actions) {
            switch (action) {
                case "IRRIGATION_ON" -> devices.setIrrigationOn(true);
                case "VENT_OPEN" -> devices.setVentOpen(true);
                case "SHADE_ON" -> devices.setShadeOn(true);
                case "LIGHT_ON" -> devices.setLightOn(true);
                case "HUMIDIFIER_ON" -> devices.setHumidifierOn(true);
            }
            ActionLog logEntry = new ActionLog();
            logEntry.setGreenhouseCode(greenhouse.getCode());
            logEntry.setTimestamp(Instant.now());
            logEntry.setAction(action);
            logEntry.setReason(reason);
            actionLogRepository.save(logEntry);
        }
        greenhouseRepository.save(greenhouse);
    }

    @Override
    public Greenhouse createDemoGreenhouseIfNotExists() {
        if (greenhouseRepository.count() > 0) {
            return greenhouseRepository.findAll().get(0);
        }

        Location demoLocation = new Location("Kecskemét", 46.8963711, 19.68996861);
        Greenhouse demo = new Greenhouse();
        demo.setCode("DEMO-1");
        demo.setName("Demo üvegház");
        demo.setLocation(demoLocation);
        demo.setPlantProfileId(null);
        demo.setPlanId(null);

        return greenhouseRepository.save(demo);
    }
}