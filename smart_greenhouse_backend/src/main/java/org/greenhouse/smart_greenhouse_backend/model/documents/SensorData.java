package org.greenhouse.smart_greenhouse_backend.model.documents;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("sensor_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorData {
    @Id
    private String id;

    private String sensorCode;
    private Instant timestamp;
    private Double temperature;  // hűréskéklet-°C
    private Double humidity;     // nedvesség-%
    private Double soilMoisture; // talajnedvesség-%
    private Double windSpeed;    // szélsebesség-m/s
}