package org.greenhouse.smart_greenhouse_backend.model.documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    private String greenhouseCode;
    private String code;
    private Instant timestamp;
    private Double temperature;  // hűréskéklet-°C
    private Double humidity;     // nedvesség-%
    private Double soilMoisture; // talajnedvesség-%
    private Double windSpeed;    // szélsebesség-km/h
}