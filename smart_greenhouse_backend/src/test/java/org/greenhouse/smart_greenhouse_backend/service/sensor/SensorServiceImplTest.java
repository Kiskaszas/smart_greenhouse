package org.greenhouse.smart_greenhouse_backend.service.sensor;

import org.greenhouse.smart_greenhouse_backend.model.documents.SensorData;
import org.greenhouse.smart_greenhouse_backend.repository.SensorDataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.core.IsInstanceOf.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensorServiceImplTest {

    @Mock
    private SensorDataRepository repository;

    @InjectMocks
    private SensorServiceImpl service;

    @Test
    void ingest_shouldSetTimestampIfMissing() {
        SensorData data = SensorData.builder()
                .id("1")
                .temperature(22.5)
                .humidity(50.0)
                .soilMoisture(40.0)
                .windSpeed(2.0)
                .build();

        when(repository.save(data)).thenAnswer(inv -> inv.getArgument(0));

        SensorData saved = service.ingest(data);

        assertNotNull(saved.getTimestamp());
        verify(repository).save(saved);
    }

    @Test
    void latest_shouldReturnMostRecentSensorData() {
        SensorData oldData = SensorData.builder()
                .id("1")
                .timestamp(Instant.now().minusSeconds(100))
                .temperature(20.0)
                .build();

        SensorData newData = SensorData.builder()
                .id("2")
                .timestamp(Instant.now())
                .temperature(25.0)
                .build();

        when(repository.findByTimestampAfter(ArgumentMatchers.<Instant>any()))
                .thenReturn(List.of(oldData, newData));

        Optional<SensorData> result = service.latest();

        assertTrue(result.isPresent());
        assertEquals("2", result.get().getId());
        assertEquals(25.0, result.get().getTemperature());
    }
}