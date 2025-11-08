package org.greenhouse.smart_greenhouse_backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.greenhouse.smart_greenhouse_backend.model.documents.Plan;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends MongoRepository<Plan, String> {

    /**
     * Egy adott üvegházhoz tartozó összes terv lekérése.
     *
     * @param greenhouseCode az üvegház azonosítója
     * @return a tervek listája
     */
    List<Plan> findByGreenhouseCode(String greenhouseCode);

    /**
     * Egy adott üvegházhoz tartozó tervek lekérése időrendben,
     * a legfrissebb elöl.
     *
     * @param greenhouseCode az üvegház azonosítója
     * @return a tervek listája időrendben
     */
    List<Plan> findByGreenhouseCodeOrderByValidFromDesc(String greenhouseCode);

    Optional<Plan> findFirstByGreenhouseCodeAndActiveTrue(String greenhouseCode);
}
