package org.greenhouse.smart_greenhouse_backend.model.documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("weather_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherSnapshot {
    @Id
    private String id;
    private String greenhouseCode;

    private Instant timestamp;
    private String city;

    private Double temperature; // °C
    private Double humidity;    // %
    private Double windSpeed;   // m/s
    private Double precipitationMm; // mm (aktuális/utolsó óra)
    private Double soilMoistureExtPct;
}