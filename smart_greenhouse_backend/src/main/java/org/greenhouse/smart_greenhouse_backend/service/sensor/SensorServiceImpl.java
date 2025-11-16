package org.greenhouse.smart_greenhouse_backend.service.sensor;

import lombok.RequiredArgsConstructor;
import org.greenhouse.smart_greenhouse_backend.model.documents.SensorData;
import org.greenhouse.smart_greenhouse_backend.repository.SensorDataRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SensorServiceImpl implements SensorService {

    private final SensorDataRepository sensorDataRepository;

    @Override
    public SensorData ingest(SensorData data) {
        if (data.getTimestamp() == null) data.setTimestamp(Instant.now());
        return sensorDataRepository.save(data);
    }

    @Override
    public Optional<SensorData> latest() {
        return sensorDataRepository.findByTimestampAfter(Instant.now().minusSeconds(7 * 24 * 3600))
                .stream().sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .findFirst();
    }

    @Override
    public void updateByCode(String sensorCode, SensorData sensor) {
        SensorData existSensor = getByCode(sensorCode);
        if (existSensor != null) {
            existSensor.setCode(sensor.getCode());
            existSensor.setTimestamp(sensor.getTimestamp());
            existSensor.setTemperature(sensor.getTemperature());
            existSensor.setHumidity(sensor.getHumidity());
            existSensor.setSoilMoisture(sensor.getSoilMoisture());
            existSensor.setWindSpeed(sensor.getWindSpeed());
            sensorDataRepository.save(existSensor);
        } else {
            throw new RuntimeException("Sensor not found: " + sensorCode);
        }
    }

    private SensorData getByCode(final String sensorCode) {
        return sensorDataRepository.findByCode(sensorCode)
                .orElseThrow(() -> new RuntimeException("Sensor not found: " + sensorCode));

    }
}