package org.greenhouse.smart_greenhouse_backend.service.greenhouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.greenhouse.smart_greenhouse_backend.dto.WeatherDto;
import org.greenhouse.smart_greenhouse_backend.exception.GreenhouseAlreadyExistsException;
import org.greenhouse.smart_greenhouse_backend.exception.GreenhouseNotFoundException;
import org.greenhouse.smart_greenhouse_backend.exception.PlanNotFoundException;
import org.greenhouse.smart_greenhouse_backend.exception.PlanNotFoundForGreenhouseException;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.DeviceState;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.Location;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.PlannedEvent;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.SensorRef;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.enums.Type;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.enums.Unit;
import org.greenhouse.smart_greenhouse_backend.model.documents.*;
import org.greenhouse.smart_greenhouse_backend.repository.*;
import org.greenhouse.smart_greenhouse_backend.service.rule_evaluator.RuleEvaluatorService;
import org.greenhouse.smart_greenhouse_backend.service.weather.WeatherService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
    public Greenhouse setActive(String code, boolean active) {
        Greenhouse greenhouse = getByCode(code);
        greenhouse.setActive(active);
        return greenhouseRepository.save(greenhouse);
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
                Objects.equals(sensorRef.id(), sensor.id())
                        && Objects.equals(sensorRef.code(), sensor.code())
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

            // 1) Külső időjárás snapshot (ahogy eddig is csináltad)
            WeatherSnapshot snapshot = WeatherSnapshot.builder()
                    .greenhouseCode(greenhouse.getCode())
                    .timestamp(Instant.now())
                    .city(weather.getCity())
                    .temperature(weather.getTemperature())
                    .humidity(weather.getHumidity())
                    .windSpeed(weather.getWindSpeed())
                    .precipitationMm(weather.getPrecipitationMm())
                    // ha van soilMoistureExtPct a WeatherDto-ban:
                    //.soilMoistureExtPct(weather.getSoilMoistureExtPct())
                    .build();
            weatherSnapshotRepository.save(snapshot);

            // 2) BELSŐ KÖRNYEZET SZIMULÁCIÓ
            simulateInternalEnvironment(greenhouse, weather);

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

    @Override
    public Greenhouse simulateNow(final String code) {
        Greenhouse greenhouse = getByCode(code);

        Location location = greenhouse.getLocation();
        if (location == null) {
            return greenhouse;
        }

        WeatherDto weather = weatherService.fetchForLocation(
                location.city(),
                location.lat(),
                location.lon()
        ).block();

        if (weather == null) {
            return greenhouse;
        }

        simulateInternalEnvironment(greenhouse, weather);

        return greenhouseRepository.save(greenhouse);
    }

    /**
     * Belső (virtuális) szenzor upsert egy üvegházhoz.
     * Ha létezik a given code, töröljük és új SensorRef-et teszünk be friss értékkel.
     */
    private void upsertInternalSensor(Greenhouse greenhouse,
                                      String sensorCode,
                                      Type type,
                                      Unit unit,
                                      double value) {
        var sensors = greenhouse.getSensors();
        if (sensors == null) {
            sensors = new ArrayList<>();
            greenhouse.setSensors(sensors);
        }

        sensors.removeIf(sr -> Objects.equals(sr.code(), sensorCode));

        SensorRef updated = new SensorRef(
                sensorCode,
                sensorCode,
                type,
                unit,
                value,
                Instant.now()
        );
        sensors.add(updated);
    }

    //Mini units
    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private double linearInterpolation(double current, double target, double alpha) {
        return current + (target - current) * alpha;
    }

    private double readInternalSensorOrDefault(Greenhouse greenhouse,
                                               String sensorCode,
                                               Type type,
                                               double defaultValue) {
        return greenhouse.getSensors().stream()
                .filter(s -> s.type() == type && sensorCode.equals(s.code()))
                .map(SensorRef::lastValue)
                .findFirst()
                .orElse(defaultValue);
    }

    private double computeInitialSoilMoisture(double extTemp, double extHumidity) {
        // nagyon egyszerű modell: a páratartalom dominál, a meleg kicsit szárít
        double base = extHumidity * 0.7 - Math.max(0, extTemp - 15) * 1.5;
        // 10–90% közé szorítjuk
        return Math.max(10.0, Math.min(90.0, base));
    }

    // #############---- SIMULATION ---######################
    private void simulateInternalEnvironment(Greenhouse greenhouse, WeatherDto weather) {
        DeviceState devices = greenhouse.getDevices();
        List<SensorRef> sensors = greenhouse.getSensors();
        Instant now = Instant.now();

        // --------- KÜLSŐ IDŐJÁRÁS (fallback alapértékekkel) ---------
        double extTemp = weather.getTemperature() != null ? weather.getTemperature() : 20.0;
        double extHumidity = weather.getHumidity() != null ? weather.getHumidity() : 60.0;

        // ---------- BELSŐ HŐMÉRSÉKLET ----------
        double intTemp = extTemp;
        if (devices.isLightOn()) {
            intTemp += 1.5;
        }
        if (devices.isVentOpen()) {
            intTemp -= 1.0;
        }
        if (devices.isShadeOn()) {
            intTemp -= 0.5;
        }

        // ---------- BELSŐ PÁRATARTALOM ----------
        double intHumidity = extHumidity;
        if (devices.isHumidifierOn()) {
            intHumidity += 5.0;
        }
        if (devices.isVentOpen()) {
            intHumidity -= 3.0;
        }

        // ---------- TALAJNEDVESSÉG (belső) ----------
        // Ha van már belső talajnedvesség szenzor, abból indulunk ki,
        // különben számolunk egy kezdeti értéket a külső adatokból.
        double soilMoist;
        Optional<SensorRef> existingSoil = sensors.stream()
                .filter(s -> "SOIL_MOIST".equals(s.code()))
                .findFirst();

        if (existingSoil.isPresent()) {
            soilMoist = existingSoil.get().lastValue();
        } else {
            soilMoist = computeInitialSoilMoisture(extTemp, extHumidity);
        }

        // Öntözés / kiszáradás hatása
        // - ha öntözés megy: jobban nő
        // - ha nem megy: nagyon lassan csökken csak
        if (devices.isIrrigationOn()) {
            // kicsit erősebb emelkedés
            soilMoist += 1.5;
            intHumidity += 0.2;
        } else {
            // nagyon lassú "párolgás", hogy ne essen össze az érték
            soilMoist -= 0.01;
        }

        // clamp 0–100% közé
        if (soilMoist < 0.0) {
            soilMoist = 0.0;
        } else if (soilMoist > 100.0) {
            soilMoist = 100.0;
        }

        // Ha elérte a 100%-ot, automatikusan kapcsoljuk le az öntözést
        if (devices.isIrrigationOn() && soilMoist >= 99.5) {
            soilMoist = 100.0;
            devices.setIrrigationOn(false);
            log.info(
                    "Öntözés automatikus kikapcsolása üvegházban {}: a talaj nedvességtartalma elérte a 100%-ot",
                    greenhouse.getCode()
            );
        }

        // ---------- SZENZOROK FRISSÍTÉSE ----------
        upsertSensor(sensors, "INT_TEMP", Type.TEMPERATURE,      Unit.CELSIUS, intTemp,     now);
        upsertSensor(sensors, "INT_HUMIDITY", Type.HUMIDITY_PCT, Unit.PERCENT, intHumidity, now);
        upsertSensor(sensors, "SOIL_MOIST",   Type.SOILMOISTURE_PTC, Unit.PERCENT, soilMoist, now);
    }

    private void upsertSensor(List<SensorRef> sensors,
                              String code,
                              Type type,
                              Unit unit,
                              Double value,
                              Instant now) {

        String id = code;

        for (SensorRef s : sensors) {
            if (code.equals(s.code())) {
                id = s.id(); // ha már van ilyen kódú szenzor, az eredeti id-t megtartjuk
                break;
            }
        }

        SensorRef updated = new SensorRef(
                id,
                code,
                type,
                unit,
                value,
                now
        );

        sensors.removeIf(s -> code.equals(s.code()));
        sensors.add(updated);
    }

    @Scheduled(fixedRateString = "${greenhouse.simulation-interval-ms:5000}")
    public void scheduledSimulation() {
        List<Greenhouse> all = greenhouseRepository.findAll();

        for (Greenhouse greenhouse : all) {
            if (!greenhouse.isActive() || greenhouse.getLocation() == null) {
                continue;
            }

            WeatherDto weather = weatherService.fetchForLocation(
                    greenhouse.getLocation().city(),
                    greenhouse.getLocation().lat(),
                    greenhouse.getLocation().lon()
            ).block();

            if (weather == null) {
                continue;
            }

            simulateInternalEnvironment(greenhouse, weather);
            greenhouseRepository.save(greenhouse);
        }
    }
}