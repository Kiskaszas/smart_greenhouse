package org.greenhouse.smart_greenhouse_backend.service.plan;

import org.greenhouse.smart_greenhouse_backend.model.documents.Plan;
import org.greenhouse.smart_greenhouse_backend.model.documents.Greenhouse;

import java.time.Instant;

public interface PlanService {

    /**
     * Visszaadja az aktív tervet, vagy létrehozza, ha nincs.
     */
    Plan getOrCreateActivePlan(final Greenhouse greenhouse);

    /**
     * Egy üres terv elkészítési az üvegházhoz.
     *
     * @param greenhouseId
     * @param validFrom
     * @param validTo
     * @return
     */
    Plan createEmptyActivePlan(final String greenhouseId,
                               final Instant validFrom,
                               final Instant validTo
    );
}
