/*package org.greenhouse.smart_greenhouse_backend.service.oldServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.greenhouse.smart_greenhouse_backend.dto.WeatherDto;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.DeviceState;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.PlannedEvent;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.SensorRef;
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

/**
 * A GreenhouseService felelős az üvegházak üzleti logikájáért.
 *
 * Tartalmazza a CRUD műveleteket, a szenzoradatok kezelését,
 * az eszközök manuális és automatikus vezérlését, az időjárás
 * lekérdezését, valamint a szabályok kiértékelését és a tervek generálását.
 *
@Service
@Slf4j
@RequiredArgsConstructor
public class GreenhouseService {

    private final GreenhouseRepository greenhouseRepository;
    private final PlantProfileRepository plantProfileRepository;
    private final ActionLogRepository actionLogRepository;
    private final PlanRepository planRepository;
    private final WeatherSnapshotRepository weatherSnapshotRepository;

    private final WeatherService weatherService;
    private final RuleEvaluatorService ruleEvaluatorService;

    /**
     * Új üvegház létrehozása.
     * Ha a plantType mező ki van töltve, automatikusan hozzárendeli
     * a megfelelő PlantProfile ID-t.
     *
     * @param greenhouse az új üvegház adatai
     * @return a mentett üvegház
     *
    public Greenhouse create(Greenhouse greenhouse) {
        // ha van plantType, hozzárendeljük a megfelelő profil ID-t
        if (greenhouse.getPlantType() != null) {
            plantProfileRepository.findByPlantType(greenhouse.getPlantType())
                    .ifPresent(profile -> greenhouse.setPlantProfileId(profile.getId()));
        }
        return greenhouseRepository.save(greenhouse);
    }

    /**
     * Az összes üvegház listázása.
     *
     * @return az összes üvegház listája
     *
    public List<Greenhouse> listAll() {
        return greenhouseRepository.findAll();
    }


    /**
     * Egy üvegház lekérése azonosító alapján.
     *
     * @param id az üvegház azonosítója
     * @return a megtalált üvegház
     * @throws RuntimeException ha nem található
     *
    public Greenhouse getById(String id) {
        return greenhouseRepository.findByCode(id)
                .orElseThrow(() -> new RuntimeException("Greenhouse not found: " + id));
    }

    /**
     * Egy üvegház adatainak frissítése.
     *
     * @param id az üvegház azonosítója
     * @param updated az új adatok
     * @return a frissített üvegház
     *
    public Greenhouse updateById(String id, Greenhouse updated) {
        Greenhouse existing = getById(id);

        existing.setName(updated.getName());
        existing.setPlantType(updated.getPlantType());
        existing.setActive(updated.isActive());
        existing.setLocation(updated.getLocation());
        existing.setSensors(updated.getSensors());
        existing.setDevices(updated.getDevices());
        existing.setPlantProfileId(updated.getPlantProfileId());
        return greenhouseRepository.save(existing);
    }

    /**
     * Egy üvegház törlése.
     *
     * @param id az üvegház azonosítója
     * @throws RuntimeException ha nem található
     *
    public void deleteById(String id) {
        if (!greenhouseRepository.existsById(id)) {
            throw new RuntimeException("Greenhouse not found: " + id);
        }
        greenhouseRepository.deleteById(id);
    }

    /**
     * Szenzoradat hozzáadása vagy frissítése egy üvegházhoz.
     *
     * @param id az üvegház azonosítója
     * @param sensor a szenzor adatai
     * @return a frissített üvegház
     *
    public Greenhouse addSensorData(String id, SensorRef sensor) {
        Greenhouse greenhouse = getById(id);
        greenhouse.getSensors().removeIf(s -> s.getId().equals(sensor.getId()));
        greenhouse.getSensors().add(sensor);
        return greenhouseRepository.save(greenhouse);
    }

    /**
     * Egy üvegház eszközeinek aktuális állapotát adja vissza.
     *
     * @param id az üvegház azonosítója
     * @return az eszközök állapota
     *
    public DeviceState getState(String id) {
        return getById(id).getDevices();
    }

    /**
     * Manuális beavatkozás végrehajtása egy üvegházban.
     * Az akció naplózásra kerül az ActionLog táblában.
     *
     * @param id az üvegház azonosítója
     * @param action a végrehajtandó akció (pl. VENT_OPEN)
     * @return az eszközök frissített állapota
     *
    public DeviceState manualAction(String id, String action) {
        Greenhouse greenhouse = getById(id);
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
        log.setGreenhouseId(id);
        log.setTs(Instant.now());
        log.setAction(action);
        log.setReason("manual");
        actionLogRepository.save(log);

        return devices;
    }

    /**
     * Aktuális időjárás lekérése egy üvegházhoz.
     * Az OpenWeather API-t hívja meg, de a city mezőt mindig
     * az üvegházban tárolt városnévre állítja.
     *
     * @param id az üvegház azonosítója
     * @return az aktuális időjárás DTO
     *
    public WeatherDto fetchWeatherForGreenhouse(String id) {
        Greenhouse greenhouse = getById(id);
        WeatherDto dto = weatherService.fetchForLocation(
                greenhouse.getLocation().getCity(),
                greenhouse.getLocation().getLat(),
                greenhouse.getLocation().getLon()
        ).block();

        // mindig a greenhouse.city-t írjuk vissza, nem az OpenWeather name mezőt
        if (dto != null) {
            dto.setCity(greenhouse.getLocation().getCity());
        }
        return dto;
    }

    /**
     * 3 napos előrejelzés lekérése egy üvegházhoz.
     *
     * @param id az üvegház azonosítója
     * @return előrejelzés WeatherDto listaként
     *
    public List<WeatherDto> fetchForecastForGreenhouse(String id) {
        Greenhouse greenhouse = getById(id);

        List<WeatherDto> forecast = weatherService.fetchForecastForLocation(
                greenhouse.getLocation().getCity(),
                greenhouse.getLocation().getLat(),
                greenhouse.getLocation().getLon()
        ).collectList().block();

        // biztosítjuk, hogy a városnév mindig az üvegházból jöjjön
        if (forecast != null) {
            forecast.forEach(dto -> dto.setCity(greenhouse.getLocation().getCity()));
        }

        return forecast;
    }

    /**
     * Egy üvegház akciónaplójának lekérése.
     *
     * @param id az üvegház azonosítója
     * @return az akciók listája időrendben
     *
    public List<ActionLog> getLogs(String id) {
        return actionLogRepository.findByGreenhouseCodeOrderByTsDesc(id);
    }

    /**
     * Szabályok kiértékelése a belső szenzoradatok alapján.
     * Ha valamelyik érték átlépi a küszöböt, a megfelelő akció
     * végrehajtásra kerül és naplózódik.
     *
    public void evaluateRulesFromSensors() {
        for (Greenhouse greenhouse : greenhouseRepository.findAll()) {
            if (!greenhouse.isActive()) continue;

            PlantProfile profile = plantProfileRepository.findByCode(greenhouse.getPlantProfileId())
                    .orElse(null);
            if (profile == null) continue;

            Map<String, Double> values = greenhouse.getSensors().stream()
                    .collect(Collectors.toMap(SensorRef::getType, SensorRef::getLastValue));

            List<String> actions = ruleEvaluatorService.evaluate(profile, values);
            applyActions(greenhouse, actions, "rule-engine");
            log.info("evaluateRulesFromSensors lefutott");
        }
    }

    /**
     * Szabályok kiértékelése az aktuális külső időjárás alapján.
     * Ha valamelyik érték átlépi a küszöböt, a megfelelő akció
     * végrehajtásra kerül és naplózódik.
     *
    public void evaluateRulesFromWeather() {
        for (Greenhouse greenhouse : greenhouseRepository.findAll()) {
            if (!greenhouse.isActive()) continue;

            // profil betöltése
            PlantProfile profile = plantProfileRepository.findByCode(greenhouse.getPlantProfileId())
                    .orElse(null);
            if (profile == null) continue;

            // időjárás lekérése (külső API)
            WeatherDto weather = weatherService.fetchForLocation(
                    greenhouse.getLocation().getCity(),
                    greenhouse.getLocation().getLat(),
                    greenhouse.getLocation().getLon()
            ).block();

            if (weather == null) continue;

            // értékek összerakása a rule engine-nek
            Map<String, Double> values = new HashMap<>();
            values.put("temperature", weather.getTemperature());
            values.put("humidity", weather.getHumidity());
            values.put("windSpeed", weather.getWindSpeed());
            if (weather.getSoilMoistureExternalPercentage() != null) {
                values.put("soilMoisturePct", weather.getSoilMoistureExternalPercentage());
            }

            // szabályok kiértékelése
            List<String> actions = ruleEvaluatorService.evaluate(profile, values);

            // akciók alkalmazása és naplózása
            applyActions(greenhouse, actions, "weather-check");
        }
        log.info("evaluateRulesFromWeather lefuttott");
    }

    /**
     * Előrejelzésből automatikusan generált terv létrehozása.
     * 12 óránként fut, és a következő 3 napra javasolt eseményeket
     * tartalmazza (pl. "Holnap 13:00 körül 32 °C, javasolt SHADE_ON").
     *
    public void generatePlans() {
        for (Greenhouse g : greenhouseRepository.findAll()) {
            if (!g.isActive()) continue;

            // Profil betöltése
            PlantProfile profile = plantProfileRepository.findByCode(g.getPlantProfileId())
                    .orElse(null);
            if (profile == null) continue;

            // Előrejelzés lekérése (pl. 3 napos forecast)
            List<WeatherDto> forecast = weatherService.fetchForecastForLocation(
                    g.getLocation().getCity(),
                    g.getLocation().getLat(),
                    g.getLocation().getLon()
            ).collectList().block();

            if (forecast == null || forecast.isEmpty()) continue;

            // Új Plan objektum létrehozása
            Plan plan = new Plan();
            plan.setGreenhouseId(g.getId());
            plan.setValidFrom(Instant.now());
            plan.setValidTo(Instant.now().plus(3, ChronoUnit.DAYS));
            plan.setActive(true);

            List<PlannedEvent> events = new ArrayList<>();

            for (WeatherDto weatherDto : forecast) {
                Map<String, Double> values = new HashMap<>();
                values.put("temperature", weatherDto.getTemperature());
                values.put("humidityPct", weatherDto.getHumidity());
                values.put("windMs", weatherDto.getWindSpeed());
                if (weatherDto.getSoilMoistureExternalPercentage() != null) {
                    values.put("soilMoisturePct", weatherDto.getSoilMoistureExternalPercentage());
                }

                List<String> actions = ruleEvaluatorService.evaluate(profile, values);
                if (!actions.isEmpty()) {
                    PlannedEvent e = new PlannedEvent();
                    e.setTs(weatherDto.getTimestamp()); // WeatherDto-ban legyen timestamp
                    e.setExpectedTemp(weatherDto.getTemperature());
                    e.setExpectedHumidity(weatherDto.getHumidity());
                    e.setSuggestedActions(actions);
                    events.add(e);
                }
            }

            plan.setExpectedEvents(events);

            // Plan mentése (PlanRepository)
            planRepository.save(plan);

            // Greenhouse-hoz hozzárendeljük az aktuális planId-t
            g.setPlanId(plan.getId());
            greenhouseRepository.save(g);
        log.info("Esemény lefutott! A javaslat: " +
                plan.toString());
        }
    }

    /**
     * Egy üvegházhoz tartozó aktuális terv lekérése.
     *
     * @param id az üvegház azonosítója
     * @return a legutóbbi generált terv
     *
    public Plan getPlanForGreenhouse(String id) {
        Greenhouse greenhouse = getById(id);

        // az adott üvegházhoz tartozó legutóbbi terv lekérése
        return planRepository.findByGreenhouseCode(greenhouse.getId()).stream()
                .filter(Plan::isActive)
                .reduce((first, second) -> second) // az utolsót tartjuk meg
                .orElseThrow(() -> new RuntimeException("Nincs terv az üvegházhoz: " + id));
    }


    /**
     * Akciók alkalmazása egy üvegházban és naplózása.
     *
     * @param greenhouse az üvegház
     * @param actions a végrehajtandó akciók
     * @param reason a végrehajtás oka (pl. "manual", "rule-engine")
     *
    private void applyActions(Greenhouse greenhouse, List<String> actions, String reason) {
        DeviceState devices = greenhouse.getDevices();
        for (String action : actions) {
            switch (action) {
                case "IRRIGATION_ON" -> devices.setIrrigationOn(true);
                case "VENT_OPEN" -> devices.setVentOpen(true);
                case "SHADE_ON" -> devices.setShadeOn(true);
                case "LIGHT_ON" -> devices.setLightOn(true);
                case "HUMIDIFIER_ON" -> devices.setHumidifierOn(true);
            }
            ActionLog log = new ActionLog();
            log.setGreenhouseId(greenhouse.getId());
            log.setTs(Instant.now());
            log.setAction(action);
            log.setReason(reason);
            actionLogRepository.save(log);
        }
        greenhouseRepository.save(greenhouse);
    }

    public void pollAllGreenhouses() {
        for (Greenhouse greenhouse : greenhouseRepository.findAll()) {
            WeatherDto weather = fetchWeatherForGreenhouse(greenhouse.getId());
            WeatherSnapshot snapshot = WeatherSnapshot.builder()
                    .greenhouseId(greenhouse.getId())
                    .timestamp(Instant.now())
                    .temperature(weather.getTemperature())
                    .humidity(weather.getHumidity())
                    .windSpeed(weather.getWindSpeed())
                    .precipitationMm(weather.getPrecipitationMm())
                    .build();
            log.info("Actual weatherSnapshot: "+snapshot.toString());
            System.out.println("fos");
            weatherSnapshotRepository.save(snapshot);
        }
    }
}*/