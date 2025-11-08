package org.greenhouse.smart_greenhouse_backend.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherDto {
    private String city;
    private Instant timestamp;
    private Double temperature;
    private Double humidity;
    private Double windSpeed;
    private Double precipitationMm;
}