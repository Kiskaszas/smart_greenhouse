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
}