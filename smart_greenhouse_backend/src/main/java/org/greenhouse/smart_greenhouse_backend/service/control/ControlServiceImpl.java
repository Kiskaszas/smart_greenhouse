package org.greenhouse.smart_greenhouse_backend.service.control;

import lombok.RequiredArgsConstructor;
import org.greenhouse.smart_greenhouse_backend.dto.ControlStateDto;
import org.greenhouse.smart_greenhouse_backend.dto.WeatherDto;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.enums.ActionType;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.enums.CommandType;
import org.greenhouse.smart_greenhouse_backend.model.documents.ControlEvent;
import org.greenhouse.smart_greenhouse_backend.repository.ControlEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ControlServiceImpl implements ControlService {

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

    @Override
    public void evaluateAndControl(String greenhouseCode ,WeatherDto weather, Double latestSoilMoisture) {
        // Öntözési logika
        if (irrigationEnabled
                && latestSoilMoisture != null && latestSoilMoisture < soilMoistureThreshold
                && weather.getTemperature() <= irrigationTempThreshold
                && weather.getWindSpeed() <= windSpeedMaxForIrrigation
                && (weather.getPrecipitationMm() == null || weather.getPrecipitationMm() == 0.0)) {
            emitEvent(
                    greenhouseCode,
                    CommandType.IRRIGATION,
                    ActionType.START,
                    "Soil moisture low; safe weather",
                    irrigationMinDuration
            );
        }

        // Szellőztetési logika
        if (ventilationEnabled
                && (weather.getTemperature() >= ventilationTempThreshold
                || weather.getHumidity() >= ventilationHumidityThreshold)) {
            emitEvent(
                    greenhouseCode,
                    CommandType.VENTILATION,
                    ActionType.START,
                    "High temp/humidity",
                    null);
        }
    }

    @Override
    public void manualCommand(
            String greenhouseCode,
            CommandType commandType,
            ActionType action,
            Integer durationMin,
            String reason
    ) {
        emitEvent(greenhouseCode, commandType, action, reason, durationMin);
    }

    private void emitEvent(
            final String greenhouseCode,
            final CommandType commandType,
            final ActionType action,
            final String reason,
            final Integer durationMin
    ) {
        ControlEvent event = ControlEvent.builder()
                .timestamp(Instant.now())
                .commandType(commandType)
                .action(action)
                .reason(reason)
                .durationMin(durationMin)
                .build();
        controlEventRepository.save(event);
        // Itt lehetne valós eszközvezérlés (GPIO, MQTT, HTTP) integráció
    }

    @Override
    public List<ControlEvent> getAllEvents() {
        return controlEventRepository.findAll();
    }

    @Override
    public ControlEvent getEventById(String id) {
        return controlEventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ControlEvent not found: " + id));
    }

    @Override
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

    @Override
    public List<ControlEvent> getAllEventsForGreenhouse(String greenhouseIgreenhouseCode) {
        return controlEventRepository.findByGreenhouseCode(greenhouseIgreenhouseCode);
    }
}
