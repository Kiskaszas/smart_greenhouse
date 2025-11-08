package org.greenhouse.smart_greenhouse_backend.repository;

import org.greenhouse.smart_greenhouse_backend.model.documents.WeatherSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface WeatherSnapshotRepository extends MongoRepository<WeatherSnapshot, String> {
    List<WeatherSnapshot> findByTimestampAfter(Instant after);
}

