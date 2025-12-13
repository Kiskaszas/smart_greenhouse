package org.greenhouse.smart_greenhouse_backend.service.greenhouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.greenhouse.smart_greenhouse_backend.config.PlantProfileLoader;
import org.greenhouse.smart_greenhouse_backend.dto.WeatherDto;
import org.greenhouse.smart_greenhouse_backend.exception.GreenhouseAlreadyExistsException;
import org.greenhouse.smart_greenhouse_backend.exception.GreenhouseNotFoundException;
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
import org.springframework.stereotype.Service;

import java.time.Duration;
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

    private final PlantProfileLoader plantProfileLoader;

    private static final Duration ACTION_COOLDOWN = Duration.ofMinutes(5); // 5 perc cooldown

    @Override
    public Greenhouse create(final Greenhouse greenhouse) {
        if (!greenhouseRepository.existsByCode(greenhouse.getCode())) {
            if (greenhouse.getPlantType() != null) {
                PlantProfile profile = plantProfileRepository.findByPlantType(greenhouse.getPlantType())
                        .orElse(ensurePlantProfileForGreenhouse(greenhouse));

                greenhouse.setPlantProfileId(profile.getId());
            }

            Greenhouse saved = greenhouseRepository.save(greenhouse);

            boolean hasPlan = planRepository.findByGreenhouseCode(saved.getCode())
                    .stream()
                    .anyMatch(Plan::isActive);

            if (!hasPlan) {
                generatePlanForNewGreenhouse(saved);
            }
            return saved;
        }
        throw new GreenhouseAlreadyExistsException("Az üvegház már létezik ezzel a kóddal");
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

    void validateSensorDatas(final String code, final SensorRef sensor) {
        if (code == null || sensor == null) {
            throw new GreenhouseNotFoundException("A megadott értékkekkel nem található a szenzor");
        }
    }

    @Override
    public Greenhouse addSensorData(final String code, final SensorRef sensor) {
        validateSensorDatas(code, sensor);

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
                (sensor.id() != null && Objects.equals(sensorRef.id(), sensor.id()))
                        || (sensor.code() != null && Objects.equals(sensorRef.code(), sensor.code()))
        );

        greenhouse.getSensors().add(sensorWithTimestamp);

        return greenhouseRepository.save(greenhouse);
    }

    @Override
    public Greenhouse updateSensorData(String code, SensorRef sensor) {
        validateSensorDatas(code, sensor);
        Greenhouse greenhouse = getByCode(code);

        int idx = -1;
        for (int i = 0; i < greenhouse.getSensors().size(); i++) {
            SensorRef s = greenhouse.getSensors().get(i);
            if (Objects.equals(s.id(), sensor.id()) || Objects.equals(s.code(), sensor.code())) {
                idx = i;
                break;
            }
        }

        if (idx == -1) {
            Greenhouse newGreenhouse = addSensorData(code, sensor);
            return newGreenhouse;
        }

        SensorRef existing = greenhouse.getSensors().get(idx);

        SensorRef updated = new SensorRef(
                sensor.id() != null ? sensor.id() : existing.id(),
                sensor.code() != null ? sensor.code() : existing.code(),
                sensor.type() != null ? sensor.type() : existing.type(),
                sensor.unit() != null ? sensor.unit() : existing.unit(),
                sensor.lastValue() != null ? sensor.lastValue() : existing.lastValue(),
                sensor.lastSeen() != null ? sensor.lastSeen() : Instant.now()
        );

        greenhouse.getSensors().set(idx, updated);

        return greenhouseRepository.save(greenhouse);
    }

    @Override
    public Greenhouse removeSensorData(String code, String sensorId) {
        Greenhouse greenhouse = getByCode(code);

        boolean removed = greenhouse.getSensors().removeIf(sensorRef ->
                (sensorId != null && Objects.equals(sensorRef.id(), sensorId))
        );

        if (removed) {
            return greenhouseRepository.save(greenhouse);
        } else {
            return greenhouse;
        }
    }

    @Override
    public DeviceState getState(final String code) {
        return getByCode(code).getDevices();
    }

    @Override
    public DeviceState manualAction(final String id, final String action) {
        // Lekérjük az üvegház objektumot azonosító (code/id) alapján
        Greenhouse greenhouse = getByCode(id);

        // Ha az üvegházhoz még nincs DeviceState példány, létrehozunk egyet
        if (greenhouse.getDevices() == null) greenhouse.setDevices(new DeviceState());

        // Lekérjük az eszközállapotot
        DeviceState devices = greenhouse.getDevices();

        // Ha még nincs inicializálva a kézi akciók időbélyegzőit tároló map, létrehozzuk
        if (devices.getLastManualActionAt() == null) devices.setLastManualActionAt(new HashMap<>());

        // Normalizáljuk az action stringet: nagybetűsítjük és levágjuk a felesleges whitespace-t
        String normalized = action == null ? "" : action.toUpperCase(Locale.ROOT).trim();

        // Meghatározzuk, melyik eszközcsoporthoz tartozik az adott akció (öntözés, szellőztetés, stb.)
        String manualKey = switch (normalized) {
            case "IRRIGATION_ON", "IRRIGATION_OFF" -> "IRRIGATION";
            case "VENT_OPEN", "VENT_CLOSE" -> "VENT";
            case "SHADE_ON", "SHADE_OFF" -> "SHADE";
            case "LIGHT_ON", "LIGHT_OFF" -> "LIGHT";
            case "HUMIDIFIER_ON", "HUMIDIFIER_OFF" -> "HUMIDIFIER";
            default -> null;
        };

        // Az akció alapján beállítjuk az adott eszköz állapotát (true = bekapcsolva/nyitva, false = kikapcsolva/zárva)
        switch (normalized) {
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
            default -> throw new IllegalArgumentException("Ismeretlen akció: " + action);
        }

        // Az adott eszközcsoporthoz eltároljuk, mikor történt utoljára kézi beavatkozás
        String key = manualKey.toUpperCase(Locale.ROOT);
        devices.getLastManualActionAt().put(key, Instant.now());

        // Az üvegház szintjén is frissítjük az utolsó akció időbélyegét
        setLastActionTimestamp(greenhouse, key, Instant.now());

        // Elmentjük az üvegház állapotát az adatbázisba
        greenhouseRepository.save(greenhouse);

        // Létrehozunk egy ActionLog bejegyzést, hogy naplózzuk a kézi akciót
        ActionLog actionLog = new ActionLog();
        actionLog.setGreenhouseCode(id);
        actionLog.setTimestamp(Instant.now());
        actionLog.setAction(action);
        actionLog.setReason("manual"); // ok: kézi beavatkozás
        actionLogRepository.save(actionLog);

        // Azonnal lefuttatjuk a szimulációt, hogy a frontend friss szenzorértékeket kapjon
        try {
            WeatherDto weather = fetchWeatherForGreenhouse(id);
            simulateInternalEnvironment(greenhouse, weather);
            greenhouseRepository.save(greenhouse);
        } catch (Exception e) {
            // Ha a szimuláció hibára fut, debug logban jelezzük
            log.debug("Szimuláció a kézi művelet után sikertelen {}: {}", id, e.getMessage());
        }

        // Visszaadjuk az aktuális eszközállapotot
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

            PlantProfile profile = plantProfileRepository.findById(greenhouse.getPlantProfileId()).orElse(null);
            if (profile == null) {
                log.warn("Nincs plant profile a greenhouse {}-hoz (id={})", greenhouse.getCode(), greenhouse.getPlantProfileId());
                continue;
            }

            Map<Type, Double> values = greenhouse.getSensors().stream()
                    .filter(Objects::nonNull)
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
        log.info("Időjárás lekérés lefutott");
    }

    @Override
    public void generatePlans() {
        for (Greenhouse greenhouse : greenhouseRepository.findAll()) {
            if (!greenhouse.isActive()) continue;
            generatePlanForNewGreenhouse(greenhouse);
        }
    }

    private PlantProfile ensurePlantProfileForGreenhouse(Greenhouse greenhouse) {
        if (greenhouse == null) return null;

        // Ha már van plantProfileId, próbáljuk meg lekérni az adatbázisból
        if (greenhouse.getPlantProfileId() != null) {
            return plantProfileRepository.findById(greenhouse.getPlantProfileId()).orElse(null);
        }

        // Ha nincs plantProfileId, de van plantType, próbáljuk meg betölteni a resources-ból
        String plantType = greenhouse.getPlantType();
        if (plantType == null || plantType.isBlank()) {
            log.debug("Nincs növénytípus az üvegházhoz {}, nem lehet betölteni a profilt az resources-ből", greenhouse.getCode());
            return null;
        }

        // Először ellenőrizzük, hogy nincs-e már DB-ben profile a plantType alapján
        Optional<PlantProfile> existing = plantProfileRepository.findByPlantType(plantType);
        if (existing.isPresent()) {
            PlantProfile p = existing.get();
            greenhouse.setPlantProfileId(p.getId());
            greenhouseRepository.save(greenhouse);
            return p;
        }

        // Ha nincs DB-ben, próbáljuk betölteni a resources/plant-profiles/{plantType}.yml fájlból
        try {
            PlantProfile loaded = plantProfileLoader.loadProfile(plantType);
            if (loaded != null) {
                // biztosítsuk, hogy a plantType/plantCode be legyen állítva (ha a loader nem tenné)
                if (loaded.getPlantType() == null) loaded.setPlantType(plantType);
                if (loaded.getPlantCode() == null) loaded.setPlantCode(plantType);

                PlantProfile saved = plantProfileRepository.save(loaded);
                greenhouse.setPlantProfileId(saved.getId());
                greenhouseRepository.save(greenhouse);
                log.info("A {} üvegház növényprofilja betöltődött a következő resources: {}", greenhouse.getCode(), plantType);
                return saved;
            } else {
                log.debug("A PlantProfileLoader null értéket adott vissza a plantType esetében. {}", plantType);
            }
        } catch (Exception e) {
            log.warn("Nem sikerült betölteni a(z) {} típus növényprofilját a resources-ből: {}", plantType, e.getMessage());
        }

        return null;
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
                    .city(weather.getCity())
                    .temperature(weather.getTemperature())
                    .humidity(weather.getHumidity())
                    .windSpeed(weather.getWindSpeed())
                    .precipitationMm(weather.getPrecipitationMm())
                    .soilMoistureExtPct(weather.getSoilMoistureExtPct())
                    .build();
            weatherSnapshotRepository.save(snapshot);

            simulateInternalEnvironment(greenhouse, weather);

            log.info("Actual weatherSnapshot: {}", snapshot);
            weatherSnapshotRepository.save(snapshot);
        }
    }

    private boolean isInCooldown(Greenhouse greenhouse, String action) {
        if (greenhouse == null || action == null) return false;
        Map<String, Instant> lastActionAt = greenhouse.getLastActionAt();
        if (lastActionAt == null) return false;
        Instant last = lastActionAt.get(action);
        if (last == null) return false;
        return Instant.now().isBefore(last.plus(ACTION_COOLDOWN));
    }

    private void setLastActionTimestamp(Greenhouse greenhouse, String action, Instant when) {
        if (greenhouse == null || action == null) return;
        if (greenhouse.getLastActionAt() == null) {
            greenhouse.setLastActionAt(new HashMap<>());
        }
        greenhouse.getLastActionAt().put(action, when);
    }

    private void applyActions(final Greenhouse greenhouse,
                              final List<String> actions,
                              final String reason) {
        if (greenhouse == null || actions == null || actions.isEmpty()) return;

        DeviceState devices = greenhouse.getDevices();
        boolean changed = false;

        for (String action : actions) {
            if (!shouldApplyAction(greenhouse, action)) {
                log.debug("A {} művelet kihagyása a következőhöz: {}, mert a shouldApplyAction értéke hamis volt", action, greenhouse.getCode());
                continue;
            }

            if (isInCooldown(greenhouse, action)) {
                log.debug("Akció kihagyása {}-ra {}-ra a lehűlési idő miatt", action, greenhouse.getCode());
                continue;
            }

            boolean applied = false;
            switch (action) {
                case "IRRIGATION_ON":
                    if (!Boolean.TRUE.equals(devices.isIrrigationOn())) {
                        devices.setIrrigationOn(true);
                        applied = true;
                    }
                    break;
                case "IRRIGATION_OFF":
                    if (!Boolean.FALSE.equals(devices.isIrrigationOn())) {
                        devices.setIrrigationOn(false);
                        applied = true;
                    }
                    break;
                case "VENT_OPEN":
                    if (!Boolean.TRUE.equals(devices.isVentOpen())) {
                        devices.setVentOpen(true);
                        applied = true;
                    }
                    break;
                case "VENT_CLOSE":
                    if (!Boolean.FALSE.equals(devices.isVentOpen())) {
                        devices.setVentOpen(false);
                        applied = true;
                    }
                    break;
                case "SHADE_ON":
                    if (!Boolean.TRUE.equals(devices.isShadeOn())) {
                        devices.setShadeOn(true);
                        applied = true;
                    }
                    break;
                case "SHADE_OFF":
                    if (!Boolean.FALSE.equals(devices.isShadeOn())) {
                        devices.setShadeOn(false);
                        applied = true;
                    }
                    break;
                case "LIGHT_ON":
                    if (!Boolean.TRUE.equals(devices.isLightOn())) {
                        devices.setLightOn(true);
                        applied = true;
                    }
                    break;
                case "LIGHT_OFF":
                    if (!Boolean.FALSE.equals(devices.isLightOn())) {
                        devices.setLightOn(false);
                        applied = true;
                    }
                    break;
                case "HUMIDIFIER_ON":
                    if (!Boolean.TRUE.equals(devices.isHumidifierOn())) {
                        devices.setHumidifierOn(true);
                        applied = true;
                    }
                    break;
                case "HUMIDIFIER_OFF":
                    if (!Boolean.FALSE.equals(devices.isHumidifierOn())) {
                        devices.setHumidifierOn(false);
                        applied = true;
                    }
                    break;
                default:
                    log.warn("Ismeretlen action: {}", action);
            }

            ActionLog logEntry = new ActionLog();
            logEntry.setGreenhouseCode(greenhouse.getCode());
            logEntry.setTimestamp(Instant.now());
            logEntry.setAction(action);
            logEntry.setReason(reason);
            actionLogRepository.save(logEntry);

            if (applied) {
                setLastActionTimestamp(greenhouse, action, Instant.now());
                changed = true;
            }
        }

        if (changed) {
            greenhouseRepository.save(greenhouse);
            log.info("Mentett üvegház {} műveletek és eszközök alkalmazása után: {}", greenhouse.getCode(), greenhouse.getDevices());
        } else {
            log.debug("Nincsenek eszközváltozások a következőhöz: {}, a mentés kihagyásra kerül.", greenhouse.getCode());
        }
    }

    private boolean shouldApplyAction(Greenhouse greenhouse, String action) {
        if (greenhouse == null || action == null) return false;
        if (greenhouse.getPlantProfileId() == null) return false;

        // Example hysteresis for irrigation only; other actions can be extended similarly
        if ("IRRIGATION_ON".equals(action)) {
            Double soil = currentSoilMoisture(greenhouse);
            if (soil == null) return false;
            return soil < 30.0;
        }
        if ("IRRIGATION_OFF".equals(action)) {
            Double soil = currentSoilMoisture(greenhouse);
            if (soil == null) return false;
            return soil > 40.0;
        }

        return true;
    }

    private Double currentSoilMoisture(Greenhouse greenhouse) {
        if (greenhouse == null) return null;
        List<SensorRef> sensors = greenhouse.getSensors();
        if (sensors == null || sensors.isEmpty()) return null;

        double sum = 0.0;
        int count = 0;

        for (SensorRef s : sensors) {
            if (s == null) continue;
            Double value = s.lastValue();
            if (value == null) continue;

            String code = s.code();
            Type type = s.type();

            boolean isSoil =
                    (code != null && code.equalsIgnoreCase("SOIL_MOIST"))
                            || (type != null && type == Type.SOILMOISTURE_PTC);

            if (isSoil) {
                sum += value;
                count++;
            }
        }

        if (count == 0) return null;
        return sum / count;
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

    private void upsertInternalSensor(Greenhouse greenhouse,
                                      String sensorCode,
                                      Type type,
                                      Unit unit,
                                      double value) {
        if (greenhouse == null || sensorCode == null) return;
        List<SensorRef> sensors = greenhouse.getSensors();
        if (sensors == null) {
            sensors = new ArrayList<>();
            greenhouse.setSensors(sensors);
        }
        String normalized = sensorCode.trim();
        sensors.removeIf(sr -> sr != null && sr.code() != null && sr.code().equalsIgnoreCase(normalized));

        SensorRef updated = new SensorRef(
                null,
                normalized,
                type,
                unit,
                value,
                Instant.now()
        );
        sensors.add(updated);
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private double linearInterpolation(double current, double target, double alpha) {
        return current + (target - current) * alpha;
    }

    private double smooth(double prev, double target, double alpha) {
        return linearInterpolation(prev, target, alpha);
    }

    private double readInternalSensorOrDefault(Greenhouse greenhouse,
                                               String sensorCode,
                                               Type type,
                                               double defaultValue) {
        return greenhouse.getSensors().stream()
                .filter(s -> s != null && s.type() == type && sensorCode.equalsIgnoreCase(s.code()))
                .map(SensorRef::lastValue)
                .findFirst()
                .orElse(defaultValue);
    }

    private void updateSensorIfExists(List<SensorRef> sensors,
                                      String code,
                                      Type type,
                                      Unit unit,
                                      Double value,
                                      Instant now) {
        if (sensors == null || code == null) return;
        for (int i = 0; i < sensors.size(); i++) {
            SensorRef s = sensors.get(i);
            if (s != null && s.code() != null && code.equalsIgnoreCase(s.code())) {
                SensorRef updated = new SensorRef(
                        s.id(),
                        s.code(), // megtartjuk az eredeti kód formátumát
                        type,
                        unit,
                        value,
                        now
                );
                sensors.set(i, updated);
                return;
            }
        }
    }

    private double computePlantUptakeFactor(
            final Greenhouse greenhouse,
            final double intTemp,
            final double currentSoilMoist, final
            PlantProfile profile
    ) {
        double base = 0.01;
        if (profile != null && profile.getSoilMoistureRangePct() != null) {
            Double min = profile.getSoilMoistureRangePct().min();
            Double max = profile.getSoilMoistureRangePct().max();
            if (min != null && max != null) {
                double desired = (min + max) / 2.0;
                double diff = Math.max(0, desired - currentSoilMoist);
                base = 0.01 + Math.min(0.05, diff / 100.0);
            }
        }
        // temperature influence
        double tempFactor = 1.0 + Math.max(0, (intTemp - 20.0)) / 50.0;
        return base * tempFactor;
    }

    private double computeInitialSoilMoisture(double extTemp, double extHumidity, double extSoilPct) {
        // simple heuristic: external soil pct adjusted by humidity and temperature
        double v = extSoilPct;
        v += (extHumidity - 50.0) * 0.05;
        v -= Math.max(0, extTemp - 20.0) * 0.2;
        return clamp(v, 0.0, 100.0);
    }

    /**
     * A szimuláció fő metódusa: frissíti a belső értékeket, upserteli a virtuális szenzorokat,
     * és alkalmazza a profilból eredő automata akciókat (manuális védelem + cooldown figyelembevételével).
     */
    private void simulateInternalEnvironment(Greenhouse greenhouse, WeatherDto weather) {
        if (greenhouse == null) return;
        if (greenhouse.getDevices() == null) greenhouse.setDevices(new DeviceState());
        if (greenhouse.getSensors() == null) greenhouse.setSensors(new ArrayList<>());

        DeviceState devices = greenhouse.getDevices();
        List<SensorRef> sensors = greenhouse.getSensors();
        Instant now = Instant.now();

        double extTemp = weather != null && weather.getTemperature() != null ? weather.getTemperature() : 20.0;
        double extWind = weather != null && weather.getWindSpeed() != null ? weather.getWindSpeed() : 0.0;
        double extSoil = weather != null && weather.getSoilMoistureExtPct() != null ? weather.getSoilMoistureExtPct() : 30.0;
        double extHum = weather != null && weather.getHumidity() != null ? weather.getHumidity() : 60.0;

        // belső értékek (eszközök hatása)
        double intTemp = extTemp + (Boolean.TRUE.equals(devices.isLightOn()) ? 1.5 : 0.0)
                - (Boolean.TRUE.equals(devices.isVentOpen()) ? 1.0 : 0.0)
                - (Boolean.TRUE.equals(devices.isShadeOn()) ? 0.5 : 0.0);

        double intWind = extWind + (Boolean.TRUE.equals(devices.isVentOpen()) ? 0.5 : 0.0);
        intWind = clamp(intWind, 0.0, 20.0);

        double intHum = extHum + (Boolean.TRUE.equals(devices.isHumidifierOn()) ? 5.0 : 0.0)
                - (Boolean.TRUE.equals(devices.isVentOpen()) ? 3.0 : 0.0);
        intHum = clamp(intHum, 0.0, 100.0);

        // talajnedvesség: prefer szenzor, különben becslés
        double soil;
        Optional<SensorRef> sSoil = sensors.stream()
                .filter(s -> s != null && s.code() != null && "SOIL_MOIST".equalsIgnoreCase(s.code()))
                .findFirst();
        if (sSoil.isPresent() && sSoil.get().lastValue() != null) soil = sSoil.get().lastValue();
        else soil = computeInitialSoilMoisture(extTemp, extHum, extSoil);

        // öntözés / evap / növényfelvétel
        if (Boolean.TRUE.equals(devices.isIrrigationOn())) {
            soil += 1.5;
            intHum += 0.2;
        } else {
            double evap = 0.01 + Math.max(0, (intTemp - 15.0)) * 0.0008 + intWind * 0.0005;
            evap *= (1.0 + Math.max(0, (50.0 - intHum)) / 200.0);
            soil -= evap;
        }

        PlantProfile profile = null;
        if (greenhouse.getPlantProfileId() != null) {
            profile = plantProfileRepository.findById(greenhouse.getPlantProfileId()).orElse(null);
        }

        double plantFactor = computePlantUptakeFactor(greenhouse, intTemp, soil, profile);
        double plantUptake = plantFactor * (1.0 + Math.max(0, (intTemp - 20.0)) / 20.0);
        soil -= plantUptake;

        // clamp profil szerint
        double soilMax = 100.0;
        double soilMin = 0.0;
        if (profile != null && profile.getSoilMoistureRangePct() != null) {
            if (profile.getSoilMoistureRangePct().max() != null) soilMax = profile.getSoilMoistureRangePct().max();
            if (profile.getSoilMoistureRangePct().min() != null) soilMin = profile.getSoilMoistureRangePct().min();
        }
        soil = clamp(soil, 0.0, 100.0);
        soil = Math.min(soil, soilMax);
        soil = Math.max(soil, soilMin);

        // ha túl nedves, automatikus kikapcsolás (ha nincs manuális védőidő)
        if (Boolean.TRUE.equals(devices.isIrrigationOn()) && profile != null && profile.getSoilMoistureRangePct() != null) {
            if (soil >= profile.getSoilMoistureRangePct().max()) {
                soil = profile.getSoilMoistureRangePct().max();
                if (!isUnderManualCooldown(devices, "IRRIGATION") && !isInCooldown(greenhouse, "IRRIGATION_OFF")) {
                    devices.setIrrigationOn(false);
                    setLastActionTimestamp(greenhouse, "IRRIGATION_OFF", now);
                    log.info("Öntözés automatikus kikapcsolása {} talaj nedvességtartalma={}", greenhouse.getCode(), soil);
                }
            }
        }

        // küszöbök profil alapján
        double tempMin = profile != null && profile.getTemperatureRange() != null && profile.getTemperatureRange().min() != null
                ? profile.getTemperatureRange().min() : -10.0;
        double tempMax = profile != null && profile.getTemperatureRange() != null && profile.getTemperatureRange().max() != null
                ? profile.getTemperatureRange().max() : 40.0;
        double humMin = profile != null && profile.getHumidityRangePct() != null && profile.getHumidityRangePct().min() != null
                ? profile.getHumidityRangePct().min() : 0.0;
        double humMax = profile != null && profile.getHumidityRangePct() != null && profile.getHumidityRangePct().max() != null
                ? profile.getHumidityRangePct().max() : 100.0;

        double humMargin = Math.max(1.0, (humMax - humMin) * 0.05);
        double humOnThreshold = Math.max(0.0, humMin + humMargin);
        double humOffThreshold = humMax;

        double soilRange = Math.max(1.0, soilMax - soilMin);
        double soilMargin = Math.max(1.0, soilRange * 0.05);
        double soilOnThreshold = Math.max(0.0, soilMin + soilMargin);
        double soilOffThreshold = soilMax;

        double tempOnForShade = tempMax + 0.5;
        double tempOffForShade = tempMin;

        // manuális védelem


        // ÖNTÖZÉS (hysteresis)
        if (!wasRecentlyManual(greenhouse, "IRRIGATION")) {
            if (Boolean.TRUE.equals(devices.isIrrigationOn())) {
                if (soil >= soilOffThreshold && !isInCooldown(greenhouse, "IRRIGATION_OFF")) {
                    devices.setIrrigationOn(false);
                    setLastActionTimestamp(greenhouse, "IRRIGATION_OFF", now);
                    log.info("Öntözés automatikus kikapcsolása {} talaj nedvességtartalma={}", greenhouse.getCode(), soil);
                }
            } else {
                if (soil <= soilOnThreshold && !isInCooldown(greenhouse, "IRRIGATION_ON")) {
                    devices.setIrrigationOn(true);
                    setLastActionTimestamp(greenhouse, "IRRIGATION_ON", now);
                    log.info("Öntözés automatikus kikapcsolása {} talaj nedvességtartalma={}", greenhouse.getCode(), soil);
                }
            }
        }

        // PÁRÁSÍTÓ
        if (!wasRecentlyManual(greenhouse, "HUMIDIFIER")) {
            if (Boolean.TRUE.equals(devices.isHumidifierOn())) {
                if (intHum >= humOffThreshold && !isInCooldown(greenhouse, "HUMIDIFIER_OFF")) {
                    devices.setHumidifierOn(false);
                    setLastActionTimestamp(greenhouse, "HUMIDIFIER_OFF", now);
                    log.info("Párásító automatikus kikapcsolása {} párásító ={}", greenhouse.getCode(), intHum);
                }
            } else {
                if (intHum <= humOnThreshold && !isInCooldown(greenhouse, "HUMIDIFIER_ON")) {
                    devices.setHumidifierOn(true);
                    setLastActionTimestamp(greenhouse, "HUMIDIFIER_ON", now);
                    log.info("Párásító automatikus kikapcsolása {} párásító ={}", greenhouse.getCode(), intHum);
                }
            }
        }

        // VILÁGÍTÁS és ÁRNYÉKOLÁS (temp alapú)
        if (!wasRecentlyManual(greenhouse, "LIGHT")) {
            if (Boolean.TRUE.equals(devices.isLightOn())) {
                if (intTemp >= tempMax && !isInCooldown(greenhouse, "LIGHT_OFF")) {
                    devices.setLightOn(false);
                    setLastActionTimestamp(greenhouse, "LIGHT_OFF", now);
                    log.info("Lámpa automatikus kikapcsolás {} hőmérséklet={}", greenhouse.getCode(), intTemp);
                }
            } else {
                if (intTemp <= tempMin && !isInCooldown(greenhouse, "LIGHT_ON")) {
                    devices.setLightOn(true);
                    setLastActionTimestamp(greenhouse, "LIGHT_ON", now);
                    log.info("Lámpa automatikus bekapcsolása {} hőmérséklet={}", greenhouse.getCode(), intTemp);
                }
            }
        }

        if (!wasRecentlyManual(greenhouse, "SHADE")) {
            if (Boolean.TRUE.equals(devices.isShadeOn())) {
                if (intTemp <= tempOffForShade && !isInCooldown(greenhouse, "SHADE_OFF")) {
                    devices.setShadeOn(false);
                    setLastActionTimestamp(greenhouse, "SHADE_OFF", now);
                    log.info("Árnyékoló automatikus kikapcsolása {} hőmérséklet={}", greenhouse.getCode(), intTemp);
                }
            } else {
                if (intTemp >= tempOnForShade && !isInCooldown(greenhouse, "SHADE_ON")) {
                    devices.setShadeOn(true);
                    setLastActionTimestamp(greenhouse, "SHADE_ON", now);
                    log.info("Árnyékoló automatikus bekapcsolása {} hőmérséklet={}", greenhouse.getCode(), intTemp);
                }
            }
        }

        // SZELLŐZTETÉS: nyit, ha bármelyik kilóg; zár, ha mindkettő visszaáll
        if (!wasRecentlyManual(greenhouse, "VENT")) {
            boolean tempOut = (intTemp < tempMin) || (intTemp > tempMax);
            boolean humOut = (intHum < humMin) || (intHum > humMax);

            if (Boolean.TRUE.equals(devices.isVentOpen())) {
                // zárás: ha semmi sem kilóg és nincs cooldown
                if (!tempOut && !humOut && !isInCooldown(greenhouse, "VENT_CLOSE")) {
                    devices.setVentOpen(false);
                    setLastActionTimestamp(greenhouse, "VENT_CLOSE", now);
                    log.info("Szellőztetés automatikus bezárása {}: hőmérséklet={}, páratartalom={}", greenhouse.getCode(), intTemp, intHum);
                }
            } else {
                // nyitás: ha bármelyik kilóg és nincs cooldown
                if ((tempOut || humOut) && !isInCooldown(greenhouse, "VENT_OPEN")) {
                    devices.setVentOpen(true);
                    setLastActionTimestamp(greenhouse, "VENT_OPEN", now);
                    log.info("Szellőztetés automatikus megnyitása {}: hőmérséklet={}, páratartalom={}", greenhouse.getCode(), intTemp, intHum);                }
            }
        }

        // SMOOTHING és upsert/update minden szenzorra
        Instant ts = now;

        Optional<SensorRef> sT = sensors.stream().filter(s -> s != null && s.code() != null && "INT_TEMP".equalsIgnoreCase(s.code())).findFirst();
        double prevT = sT.map(SensorRef::lastValue).orElse(Double.NaN);
        double smoothT = Double.isNaN(prevT) ? intTemp : smooth(prevT, intTemp, 0.2);
        if (sT.isPresent()) updateSensorIfExists(sensors, sT.get().code(), Type.TEMPERATURE, Unit.CELSIUS, smoothT, ts);
        else upsertInternalSensor(greenhouse, "INT_TEMP", Type.TEMPERATURE, Unit.CELSIUS, smoothT);

        Optional<SensorRef> sH = sensors.stream().filter(s -> s != null && s.code() != null && "INT_HUMIDITY".equalsIgnoreCase(s.code())).findFirst();
        double prevH = sH.map(SensorRef::lastValue).orElse(Double.NaN);
        double smoothH = Double.isNaN(prevH) ? intHum : smooth(prevH, intHum, 0.15);
        if (sH.isPresent()) updateSensorIfExists(sensors, sH.get().code(), Type.HUMIDITY_PCT, Unit.PERCENT, smoothH, ts);
        else upsertInternalSensor(greenhouse, "INT_HUMIDITY", Type.HUMIDITY_PCT, Unit.PERCENT, smoothH);

        if (sSoil.isPresent()) updateSensorIfExists(sensors, sSoil.get().code(), Type.SOILMOISTURE_PTC, Unit.PERCENT, soil, ts);
        else upsertInternalSensor(greenhouse, "SOIL_MOIST", Type.SOILMOISTURE_PTC, Unit.PERCENT, soil);

        Optional<SensorRef> sW = sensors.stream().filter(s -> s != null && s.code() != null && "WIND_SPEED".equalsIgnoreCase(s.code())).findFirst();
        double prevW = sW.map(SensorRef::lastValue).orElse(Double.NaN);
        double smoothW = Double.isNaN(prevW) ? intWind : smooth(prevW, intWind, 0.2);
        if (sW.isPresent()) updateSensorIfExists(sensors, sW.get().code(), Type.WIND_SPEED, Unit.KILO_METER_PER_HOUR, smoothW, ts);
        else upsertInternalSensor(greenhouse, "WIND_SPEED", Type.WIND_SPEED, Unit.KILO_METER_PER_HOUR, smoothW);

        // profil szabályok smoothed értékekkel
        evaluatePlantRules(greenhouse, profile, smoothT, smoothH, soil, smoothW);

        greenhouseRepository.save(greenhouse);
        log.debug("Szimuláció {} -> hőmérséklet={}, páratartalom={}, talajnedvesség={}, szélsebesség={}, eszközök={}",
                greenhouse.getCode(), smoothT, smoothH, soil, smoothW, greenhouse.getDevices());
    }

    private boolean wasRecentlyManual(Greenhouse greenhouse, String groupKey) {
        if (greenhouse == null || greenhouse.getDevices() == null) return false;
        Map<String, Instant> lastManual = greenhouse.getDevices().getLastManualActionAt();
        if (lastManual == null) return false;
        String key = groupKey == null ? null : groupKey.toUpperCase(Locale.ROOT);
        if (key == null) return false;
        Instant last = lastManual.get(key);
        return last != null && Instant.now().isBefore(last.plus(ACTION_COOLDOWN));
    }

    private boolean isUnderManualCooldown(DeviceState devices, String groupKey) {
        if (devices == null || devices.getLastManualActionAt() == null || groupKey == null) return false;
        Instant last = devices.getLastManualActionAt().get(groupKey);
        if (last == null) return false;
        return Instant.now().isBefore(last.plus(ACTION_COOLDOWN));
    }

    private void evaluatePlantRules(Greenhouse greenhouse,
                                    PlantProfile profile,
                                    double temp,
                                    double hum,
                                    double soil,
                                    double wind) {
        if (greenhouse == null || profile == null) return;

        Map<Type, Double> values = new HashMap<>();
        values.put(Type.TEMPERATURE, temp);
        values.put(Type.HUMIDITY_PCT, hum);
        values.put(Type.SOILMOISTURE_PTC, soil);
        values.put(Type.WIND_SPEED, wind);

        List<String> actions = ruleEvaluatorService.evaluate(profile, values);
        if (actions != null && !actions.isEmpty()) {
            applyActions(greenhouse, actions, "profile-rules");
        }
    }
}
//x