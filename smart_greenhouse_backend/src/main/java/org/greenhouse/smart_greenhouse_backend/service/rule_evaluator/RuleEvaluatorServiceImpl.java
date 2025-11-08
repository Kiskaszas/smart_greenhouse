package org.greenhouse.smart_greenhouse_backend.service.rule_evaluator;

import lombok.RequiredArgsConstructor;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.ActionRule;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.Range;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.enums.Type;
import org.greenhouse.smart_greenhouse_backend.model.documents.PlantProfile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RuleEvaluatorServiceImpl implements RuleEvaluatorService {

    @Override
    public List evaluate(PlantProfile profile, Map<Type, Double> values) {
        List actions = new ArrayList();

        if (profile.getRules() == null) {
            return actions;
        }

        for (ActionRule rule : profile.getRules()) {
            Double measured = values.get(rule.metric());
            if (measured != null) {
                Range range = rule.range();
                if (range != null && (measured < range.min() || measured > range.max())) {
                    actions.add(rule.action());
                }
            }
        }
        return actions;
    }
}
