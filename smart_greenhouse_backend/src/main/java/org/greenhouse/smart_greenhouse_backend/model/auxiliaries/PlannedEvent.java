package org.greenhouse.smart_greenhouse_backend.model.auxiliaries;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlannedEvent {
    private Instant at;                  // mikor fusson
    private Double expectedTemperature;  // előrejelzett hőmérséklet
    private Double expectedHumidity;     // előrejelzett páratartalom
    private Double expectedWindSpeed;    // előrejelzett szél
    private Double expectedSoilMoisture; // előrejelzett talajnedvesség (ha van)

    private List<String> suggestedActions; // pl. ["IRRIGATION_START", "VENT_OPEN"]

    private String type;         // IRRIGATION / VENTILATION / SHADE
    private String action;       // START / STOP
    private Integer durationMin; // ha START, hány perc
    private String reason;       // pl. "rule evaluation"
}