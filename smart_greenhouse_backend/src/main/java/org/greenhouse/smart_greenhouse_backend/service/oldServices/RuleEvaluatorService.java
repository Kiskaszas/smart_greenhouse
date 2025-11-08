/*package org.greenhouse.smart_greenhouse_backend.service.oldServices;

import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.ActionRule;
import org.greenhouse.smart_greenhouse_backend.model.documents.PlantProfile;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.Range;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Szabálymotor, amely a növényprofilban definiált szabályokat
 * kiértékeli az aktuális mért értékek alapján.
 *
@Service
public class RuleEvaluatorService {

    /**
     * Kiértékeli a szabályokat egy adott növényprofil és aktuális értékek alapján.
     *
     * @param profile a növény profilja (benne a szabályok)
     * @param values aktuális mért értékek (pl. tempC, humidityPct, soilMoisturePct)
     * @return végrehajtandó akciók listája
     *
    public List<String> evaluate(PlantProfile profile, Map<String, Double> values) {
        List<String> actions = new ArrayList<>();

        if (profile.getRules() == null) return actions;

        for (ActionRule rule : profile.getRules()) {
            Double measured = values.get(rule.getMetric());
            if (measured != null) {
                Range r = rule.getRange();
                if (r != null && (measured < r.getMin() || measured > r.getMax())) {
                    actions.add(rule.getAction());
                }
            }
        }
        return actions;
    }
}*/