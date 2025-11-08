package org.greenhouse.smart_greenhouse_backend.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Az aktuális vezérlési konfiguráció DTO-ja.
 * Megmutatja, hogy mely funkciók engedélyezettek és milyen küszöbértékek vannak beállítva.
 */
@Data
@Builder
public class ControlStateDto {
    private boolean irrigationEnabled;
    private boolean ventilationEnabled;

    private double soilMoistureThreshold;
    private double irrigationTempThreshold;
    private double windSpeedMaxForIrrigation;
    private int irrigationMinDuration;

    private double ventilationTempThreshold;
    private double ventilationHumidityThreshold;
}