package org.greenhouse.smart_greenhouse_backend.service.plan;

import org.greenhouse.smart_greenhouse_backend.model.documents.Greenhouse;
import org.greenhouse.smart_greenhouse_backend.model.documents.Plan;
import org.greenhouse.smart_greenhouse_backend.repository.PlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceImplTest {

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private PlanServiceImpl planService;

    @Test
    void getOrCreateActivePlan_shouldReturnExistingPlan() {
        Greenhouse greenhouse = new Greenhouse();
        greenhouse.setId("greenhouse1");

        Plan existingPlan = Plan.builder()
                .id("plan1")
                .greenhouseCode("greenhouse1")
                .active(true)
                .build();

        when(planRepository.findFirstByGreenhouseCodeAndActiveTrue("greenhouse1"))
                .thenReturn(Optional.of(existingPlan));

        Plan result = planService.getOrCreateActivePlan(greenhouse);

        assertEquals("plan1", result.getId());
        verify(planRepository, never()).save(any());
    }

    @Test
    void getOrCreateActivePlan_shouldSeedDefaultPlanWhenNoneExists() {
        Greenhouse greenhouse = new Greenhouse();
        greenhouse.setId("greenhouse2");

        when(planRepository.findFirstByGreenhouseCodeAndActiveTrue("greenhouse2"))
                .thenReturn(Optional.empty());

        when(planRepository.save(any(Plan.class)))
                .thenAnswer(inv -> {
                    Plan p = inv.getArgument(0);
                    p.setId("generatedPlan");
                    return p;
                });

        Plan result = planService.getOrCreateActivePlan(greenhouse);

        assertNotNull(result.getId());
        assertEquals("greenhouse2", result.getGreenhouseCode());
        assertTrue(result.isActive());
        assertFalse(result.getEvents().isEmpty());
        assertEquals("IRRIGATION", result.getEvents().get(0).getType());
    }

    @Test
    void createEmptyActivePlan_shouldSaveAndReturnPlan() {
        Instant from = Instant.now();
        Instant to = Instant.now().plus(365, ChronoUnit.DAYS);

        Plan plan = Plan.builder()
                .id("planX")
                .greenhouseCode("greenhouse3")
                .validFrom(from)
                .validTo(to)
                .active(true)
                .build();

        when(planRepository.save(any(Plan.class))).thenReturn(plan);

        Plan result = planService.createEmptyActivePlan("greenhouse3");

        assertEquals("planX", result.getId());
        assertEquals("greenhouse3", result.getGreenhouseCode());
        assertTrue(result.isActive());
        assertEquals(from, result.getValidFrom());
        assertEquals(to, result.getValidTo());
    }
}