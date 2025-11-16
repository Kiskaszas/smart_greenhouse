package org.greenhouse.smart_greenhouse_backend.repository;

import org.greenhouse.smart_greenhouse_backend.model.documents.SensorData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SensorDataRepository extends MongoRepository<SensorData, String> {
    List<SensorData> findByTimestampAfter(Instant after);

    Optional<SensorData> findByCode(String sensorCode);
}