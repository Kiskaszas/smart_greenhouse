package org.greenhouse.smart_greenhouse_backend.service.plan;

import lombok.RequiredArgsConstructor;
import org.greenhouse.smart_greenhouse_backend.model.documents.Plan;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.PlannedEvent;
import org.greenhouse.smart_greenhouse_backend.model.documents.Greenhouse;

import org.greenhouse.smart_greenhouse_backend.repository.PlanRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;

    @Value("${irrigation.min-duration-minutes:5}")
    private int defaultIrrigationMinutes;

    @Override
    public Plan getOrCreateActivePlan(Greenhouse greenhouse) {
        return planRepository.findFirstByGreenhouseCodeAndActiveTrue(greenhouse.getId())
                .orElseGet(() -> seedDefaultPlan(greenhouse));
    }

    private Plan seedDefaultPlan(Greenhouse gh) {
        Instant now = Instant.now();
        PlannedEvent e = PlannedEvent.builder()
                .at(now.plus(5, ChronoUnit.MINUTES))
                .type("IRRIGATION")
                .action("START")
                .durationMin(defaultIrrigationMinutes)
                .reason("default plan seed")
                .build();

        Plan plan = Plan.builder()
                .greenhouseCode(gh.getId())
                .validFrom(now)
                .validTo(now.plus(3, ChronoUnit.DAYS))
                .active(true)
                .events(List.of(e))
                .build();

        return planRepository.save(plan);
    }

    @Override
    public Plan createEmptyActivePlan(
            final String greenhouseId,
            final Instant validFrom,
            final Instant validTo
    ) {
        Plan plan = Plan.builder()
                .greenhouseCode(greenhouseId)
                .validFrom(validFrom)
                .validTo(validTo)
                .active(true)
                .build();
        return planRepository.save(plan);
    }
}
