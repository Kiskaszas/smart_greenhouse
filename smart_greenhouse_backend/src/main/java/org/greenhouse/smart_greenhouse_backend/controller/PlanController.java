package org.greenhouse.smart_greenhouse_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.greenhouse.smart_greenhouse_backend.model.documents.Greenhouse;
import org.greenhouse.smart_greenhouse_backend.model.documents.Plan;
import org.greenhouse.smart_greenhouse_backend.service.greenhouse.GreenhouseService;
import org.greenhouse.smart_greenhouse_backend.service.plan.PlanService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/greenhouses/plan")
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

    @Operation(summary = "Új inaktív terv létrehozása a megadott +1 évre")
    @GetMapping("/create/{greenhouseCode}")
    public Plan createPlan(
            @PathVariable("greenhouseCode") String greenhouseCode
    ) {
        return planService.createEmptyInActivePlan(greenhouseCode);
    }

    @Operation(summary = "Az aktuális terv aktiválása a megadott üvegházhoz.")
    @GetMapping("/to-active/{greenhouseCode}")
    public Plan toActivatePlan(@PathVariable("greenhouseCode") String greenhouseCode) {
        return planService.toActivatePlanForGeenhouse(greenhouseCode);
    }

    @Operation(summary = "Terv törlése az adott üvegház kódja alapján.")
    @DeleteMapping("/delete/{greenhouseCode}")
    public void deletePlan(@PathVariable("greenhouseCode") String greenhouseCode) {
        planService.deletePlanByGreenhouseCode(greenhouseCode);
    }
}