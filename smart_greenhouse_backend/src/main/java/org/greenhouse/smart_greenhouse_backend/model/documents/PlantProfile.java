package org.greenhouse.smart_greenhouse_backend.model.documents;

import lombok.Data;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.ActionRule;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.Range;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Egy növény profilja, amely tartalmazza a szabályokat
 * (ActionRule) az optimális környezeti feltételekhez.
 */
@Data
@Document("plantProfiles")
public class PlantProfile {
    @Id
    private String id;

    private String plantCode;
    private String plantType;

    private Range temperatureRange;
    private Range humidityRangePct;
    private Range soilMoistureRangePct;

    private List<ActionRule> rules;
}
