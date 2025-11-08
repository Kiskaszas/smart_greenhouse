package org.greenhouse.smart_greenhouse_backend.service.rule_evaluator;

import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.ActionRule;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.Range;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.enums.Type;
import org.greenhouse.smart_greenhouse_backend.model.documents.PlantProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class RuleEvaluatorServiceImplTest {

    private RuleEvaluatorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RuleEvaluatorServiceImpl();
    }

    @Test
    void evaluate_shouldReturnEmptyList_whenNoRules_testCase1() {
        PlantProfile profile = new PlantProfile();
        profile.setRules(null);

        Map<Type, Double> values = Map.of(Type.TEMPERATURE, 25.0);

        List<String> result = service.evaluate(profile, values);

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluate_shouldReturnAction_whenValueOutsideRange_testCase1() {
        Range range = new Range(20.0, 30.0);
        ActionRule rule = new ActionRule(Type.TEMPERATURE, range, "VENT_OPEN");

        PlantProfile profile = new PlantProfile();
        profile.setRules(List.of(rule));

        Map<Type, Double> values = Map.of(Type.TEMPERATURE, 35.0);

        List<String> result = service.evaluate(profile, values);

        assertEquals(1, result.size());
        assertEquals("VENT_OPEN", result.get(0));
    }

    @Test
    void evaluate_shouldReturnAction_whenValueOutsideRange_testCase2() {
        Range range = new Range(20.0, 30.0);
        ActionRule rule = new ActionRule(Type.TEMPERATURE, range, "VENT_OPEN");

        PlantProfile profile = new PlantProfile();
        profile.setRules(List.of(rule));

        Map<Type, Double> values = Map.of(Type.TEMPERATURE, 35.0);

        List<String> result = service.evaluate(profile, values);

        assertEquals(1, result.size());
        assertEquals("VENT_OPEN", result.get(0));
    }

    @Test
    void evaluate_shouldReturnEmptyList_whenValueWithinRange_testCase1() {
        Range range = new Range(20.0, 30.0);
        ActionRule rule = new ActionRule(Type.TEMPERATURE, range, "VENT_OPEN");

        PlantProfile profile = new PlantProfile();
        profile.setRules(List.of(rule));

        Map<Type, Double> values = Map.of(Type.TEMPERATURE, 25.0);

        List<String> result = service.evaluate(profile, values);

        assertTrue(result.isEmpty());
    }
}