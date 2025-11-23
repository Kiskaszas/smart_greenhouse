package org.greenhouse.smart_greenhouse_backend.repository;

import org.greenhouse.smart_greenhouse_backend.model.documents.Plan;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository a {@link Plan} dokumentumok tárolásához és lekérdezéséhez.
 * Tartalmaz egyszerű lekérdezéseket üvegház kód alapján, valamint törlési műveletet.
 */
@Repository
public interface PlanRepository extends MongoRepository<Plan, String> {

    /**
     * Egy adott üvegházhoz tartozó összes terv lekérése.
     *
     * @param greenhouseCode az üvegház azonosítója
     * @return a megadott üvegházhoz tartozó tervek listája; üres lista, ha nincs terv
     */
    List<Plan> findByGreenhouseCode(String greenhouseCode);

    /**
     * Egy adott üvegházhoz tartozó tervek lekérése időrendben, a legfrissebb elemmel az első helyen.
     *
     * @param greenhouseCode az üvegház azonosítója
     * @return a megadott üvegházhoz tartozó tervek listája rendezve {@code validFrom} szerint csökkenő sorrendben
     */
    List<Plan> findByGreenhouseCodeOrderByValidFromDesc(String greenhouseCode);

    /**
     * Az adott üvegházhoz tartozó első aktív terv lekérése.
     *
     * @param greenhouseCode az üvegház azonosítója
     * @return az aktív {@link Plan}, ha létezik; {@link Optional#empty()} ha nincs aktív terv
     */
    Optional<Plan> findFirstByGreenhouseCodeAndActiveTrue(String greenhouseCode);

    /**
     * Az adott üvegházhoz tartozó első inaktív (nem aktív) terv lekérése.
     *
     * @param greenhouseCode az üvegház azonosítója
     * @return egy inaktív {@link Plan}, ha van; {@link Optional#empty()} ha nincs ilyen terv
     */
    Optional<Plan> findPlanByGreenhouseCodeAndActiveIsFalse(String greenhouseCode);

    /**
     * Az összes terv törlése az adott üvegházhoz tartozóan.
     *
     * @param greenhouseCode az üvegház azonosítója; minden ehhez a kódhoz tartozó {@link Plan} törlődik
     */
    void deleteByGreenhouseCode(String greenhouseCode);
}