package org.greenhouse.smart_greenhouse_backend.repository;

import org.greenhouse.smart_greenhouse_backend.model.documents.PlantProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlantProfileRepository extends MongoRepository<PlantProfile, String> {

    Optional<PlantProfile> findByPlantType(String plantType);

    Optional<PlantProfile> findByPlantCode(String plantCode);
}
