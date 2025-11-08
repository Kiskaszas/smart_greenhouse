package org.greenhouse.smart_greenhouse_backend.repository;

import org.greenhouse.smart_greenhouse_backend.model.documents.ActionLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionLogRepository extends MongoRepository<ActionLog, String> {

    List<ActionLog> findActionLogsByGreenhouseCode(String code);

    ActionLog findActionLogByGreenhouseCode(String code);
}
