package org.greenhouse.smart_greenhouse_backend.repository;

import org.greenhouse.smart_greenhouse_backend.model.documents.Greenhouse;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GreenhouseRepository extends MongoRepository<Greenhouse, String> {

    Optional<Greenhouse> findByCode(String code);
    Optional<Greenhouse> findByName(String name);
    List<Greenhouse> findByLocationCity(String city);

    boolean existsByCode(String code);
}
