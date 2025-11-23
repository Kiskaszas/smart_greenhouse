package org.greenhouse.smart_greenhouse_backend.service.plan;

import org.greenhouse.smart_greenhouse_backend.model.documents.Greenhouse;
import org.greenhouse.smart_greenhouse_backend.model.documents.Plan;

public interface PlanService {

    /**
     * Visszaadja az aktív tervet, vagy létrehozza, ha nincs.
     */
    Plan getOrCreateActivePlan(final Greenhouse greenhouse);

    /**
     * Egy üres inaktív terv elkészítés az üvegházhoz.
     *
     * @param greenhouseCode az üvegház azonosítója.
     * @return egy plan objektum.
     */
    Plan createEmptyInActivePlan(final String greenhouseCode);

    /**
     * Egy terv státusztának aktivvá állítása az adott üvegházhoz.
     *
     * @param greenhouseCode az üvegház azonosítója.
     * @return egy plan objektum.
     */
    Plan toActivatePlanForGeenhouse(final String greenhouseCode);

    /**
     * Egy terv törlése az üvegházhoz.
     *
     * @param greenhouseCode az üvegház azonosítója.
     */
    void deletePlanByGreenhouseCode(final String greenhouseCode);
}
