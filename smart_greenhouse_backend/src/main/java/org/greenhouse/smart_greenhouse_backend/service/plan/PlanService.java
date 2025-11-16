package org.greenhouse.smart_greenhouse_backend.service.plan;

import org.greenhouse.smart_greenhouse_backend.model.documents.Greenhouse;
import org.greenhouse.smart_greenhouse_backend.model.documents.Plan;

public interface PlanService {

    /**
     * Visszaadja az aktív tervet, vagy létrehozza, ha nincs.
     */
    Plan getOrCreateActivePlan(final Greenhouse greenhouse);

    /**
     * Egy üres terv elkészítési az üvegházhoz.
     *
     * @param greenhouseCode
     * @return
     */
    Plan createEmptyActivePlan(final String greenhouseCode);
}
