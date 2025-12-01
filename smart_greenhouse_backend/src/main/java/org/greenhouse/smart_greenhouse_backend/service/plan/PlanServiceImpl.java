package org.greenhouse.smart_greenhouse_backend.service.plan;

import lombok.RequiredArgsConstructor;
import org.greenhouse.smart_greenhouse_backend.exception.PlanNotFoundForGreenhouseException;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.PlannedEvent;
import org.greenhouse.smart_greenhouse_backend.model.documents.Greenhouse;
import org.greenhouse.smart_greenhouse_backend.model.documents.Plan;
import org.greenhouse.smart_greenhouse_backend.repository.PlanRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;

    @Value("${irrigation.min-duration-minutes:5}")
    private int defaultIrrigationMinutes;

    @Override
    public Plan getOrCreateActivePlan(Greenhouse greenhouse) {
        return planRepository.findFirstByGreenhouseCodeAndActiveTrue(greenhouse.getCode())
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
                .greenhouseCode(gh.getCode())
                .validFrom(now)
                .validTo(now.plus(3, ChronoUnit.DAYS))
                .active(true)
                .events(List.of(e))
                .build();

        return planRepository.save(plan);
    }

    @Override
    public Plan createEmptyInActivePlan(
            final String greenhouseCode) {
        Plan plan = Plan.builder()
                .greenhouseCode(greenhouseCode)
                .validFrom(Instant.now())
                .validTo(Instant.now().plus(365, ChronoUnit.DAYS))
                .active(false)
                .build();
        return planRepository.save(plan);
    }

    @Override
    public Plan toActivatePlanForGeenhouse(final String greenhouseCode) {
        Optional<Plan> plan = planRepository.findPlanByGreenhouseCodeAndActiveIsFalse(greenhouseCode);
        if (plan.isPresent()) {
            Plan existPlan = plan.get();
            existPlan.setActive(true);
            return planRepository.save(existPlan);
        }else{
            throw new PlanNotFoundForGreenhouseException(greenhouseCode);
        }
    }

    @Override
    public void deletePlanByGreenhouseCode(String greenhouseCode) {
        List<Plan> existsPlan = planRepository.findByGreenhouseCode(greenhouseCode);
        if (existsPlan.isEmpty()) {
            throw new PlanNotFoundForGreenhouseException(greenhouseCode);
        }
        planRepository.deleteByGreenhouseCode(greenhouseCode);
    }
}
