/*package org.greenhouse.smart_greenhouse_backend.service.oldServices;

import org.greenhouse.smart_greenhouse_backend.dto.ControlStateDto;
import org.greenhouse.smart_greenhouse_backend.dto.WeatherDto;
import org.greenhouse.smart_greenhouse_backend.model.documents.ControlEvent;
import org.greenhouse.smart_greenhouse_backend.repository.ControlEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ControlService {

    private final ControlEventRepository controlEventRepository;

    @Value("${irrigation.enabled:true}")
    private boolean irrigationEnabled;
    @Value("${ventilation.enabled:true}")
    private boolean ventilationEnabled;

    @Value("${irrigation.threshold.soilMoisture:35}")
    private double soilMoistureThreshold;
    @Value("${irrigation.threshold.temperature:28}")
    private double irrigationTempThreshold;
    @Value("${irrigation.threshold.windSpeed:8}")
    private double windSpeedMaxForIrrigation;
    @Value("${irrigation.min-duration-minutes:5}")
    private int irrigationMinDuration;

    @Value("${ventilation.threshold.temperature:28}")
    private double ventilationTempThreshold;
    @Value("${ventilation.threshold.humidity:80}")
    private double ventilationHumidityThreshold;

    public void evaluateAndControl(
            final WeatherDto weather,
            final Double latestSoilMoisture
    ) {
        // Öntözési logika (példa)
        if (irrigationEnabled
                && latestSoilMoisture != null && latestSoilMoisture < soilMoistureThreshold
                && weather.getTemperature() <= irrigationTempThreshold
                && weather.getWindSpeed() <= windSpeedMaxForIrrigation
                && (weather.getPrecipitationMm() == null || weather.getPrecipitationMm() == 0.0)) {
            emitEvent("IRRIGATION", "START", "Soil moisture low; safe weather", irrigationMinDuration);
        }

        // Szellőztetési logika (példa)
        if (ventilationEnabled
                && (weather.getTemperature() >= ventilationTempThreshold
                || weather.getHumidity() >= ventilationHumidityThreshold)) {
            emitEvent("VENTILATION", "START", "High temp/humidity", null);
        }
    }

    public void manualCommand(
            final String type,
            final String action,
            final Integer durationMin,
            final String reason
    ) {
        emitEvent(type, action, reason, durationMin);
    }

    private void emitEvent(final String type,
                           final String action,
                           final String reason,
                           final Integer durationMin
    ) {
        ControlEvent event = ControlEvent.builder()
                .timestamp(Instant.now())
                .type(type)
                .action(action)
                .reason(reason)
                .durationMin(durationMin)
                .build();
        controlEventRepository.save(event);
        // Itt lehetne valós eszközvezérlés (GPIO, MQTT, HTTP) integráció
    }

    /**
     * Összes vezérlési esemény lekérése.
     *
    public List<ControlEvent> getAllEvents() {
        return controlEventRepository.findAll();
    }

    /**
     * Egy vezérlési esemény lekérése ID alapján.
     *
    public ControlEvent getEventById(String id) {
        return controlEventRepository.findByCode(id)
                .orElseThrow(() -> new RuntimeException("ControlEvent not found: " + id));
    }

    /**
     * Aktuális vezérlési konfiguráció visszaadása DTO-ban.
     *
    public ControlStateDto getControlState() {
        return ControlStateDto.builder()
                .irrigationEnabled(irrigationEnabled)
                .ventilationEnabled(ventilationEnabled)
                .soilMoistureThreshold(soilMoistureThreshold)
                .irrigationTempThreshold(irrigationTempThreshold)
                .windSpeedMaxForIrrigation(windSpeedMaxForIrrigation)
                .irrigationMinDuration(irrigationMinDuration)
                .ventilationTempThreshold(ventilationTempThreshold)
                .ventilationHumidityThreshold(ventilationHumidityThreshold)
                .build();
    }
}*/