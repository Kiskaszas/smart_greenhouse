package org.greenhouse.smart_greenhouse_backend.repository;

import org.greenhouse.smart_greenhouse_backend.model.documents.ControlEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ControlEventRepository extends MongoRepository<ControlEvent, String> {

    List<ControlEvent> findByGreenhouseCode(String greenhouseId);
}