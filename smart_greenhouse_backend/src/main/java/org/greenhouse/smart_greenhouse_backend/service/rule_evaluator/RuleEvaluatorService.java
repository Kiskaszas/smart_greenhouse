package org.greenhouse.smart_greenhouse_backend.service.rule_evaluator;

import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.enums.Type;
import org.greenhouse.smart_greenhouse_backend.model.documents.PlantProfile;

import java.util.List;
import java.util.Map;

public interface RuleEvaluatorService {

    /**
     * Kiértékeli a szabályokat egy adott növényprofil és aktuális értékek alapján.
     *
     * @param profile a növény profilja (benne a szabályok)
     * @param values  aktuális mért értékek (pl. tempC, humidityPct, soilMoisturePct)
     * @return String akciók listája
     */
    List evaluate(final PlantProfile profile, final Map<Type, Double> values);
}
