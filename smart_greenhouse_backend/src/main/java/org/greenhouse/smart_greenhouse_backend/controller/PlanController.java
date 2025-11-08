package org.greenhouse.smart_greenhouse_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.greenhouse.smart_greenhouse_backend.model.documents.Greenhouse;
import org.greenhouse.smart_greenhouse_backend.model.documents.Plan;
import org.greenhouse.smart_greenhouse_backend.service.greenhouse.GreenhouseService;
import org.greenhouse.smart_greenhouse_backend.service.plan.PlanService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/greenhouses/{id}/plans")
@RequiredArgsConstructor
public class PlanController {

    private final GreenhouseService greenhouseService;
    private final PlanService planService;

    @Operation(summary = "Aktív terv lekérése (ha nincs, létrehozza)")
    @GetMapping("/active/{greenhouseCode}")
    public Plan getOrCreateActive(@PathVariable("greenhouseCode") String greenhouseCode) {
        Greenhouse greenhouse = greenhouseService.getByCode(greenhouseCode);
        return planService.getOrCreateActivePlan(greenhouse);
    }

    @Operation(summary = "Új aktív terv létrehozása a megadott időtartamra")
    @PostMapping
    public Plan createPlan(
            @PathVariable("greenhouseId") String greenhouseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return planService.createEmptyActivePlan(greenhouseId, from, to);
    }
}