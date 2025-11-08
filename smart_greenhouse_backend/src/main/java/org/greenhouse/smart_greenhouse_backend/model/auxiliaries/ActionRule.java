package org.greenhouse.smart_greenhouse_backend.model.auxiliaries;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.enums.Type;

/**
 * Egy szabály, amely meghatározza, hogy egy adott mért érték
 * (metric) milyen tartományban optimális, és ha kilép belőle,
 * milyen akciót kell végrehajtani.
 */
public record ActionRule(
        Type metric,   // pl. "temperature", "humidityPct", "soilMoisturePct"
        Range range,     // megengedett tartomány
        String action  // pl. "VENT_OPEN", "IRRIGATION_ON"
){
}