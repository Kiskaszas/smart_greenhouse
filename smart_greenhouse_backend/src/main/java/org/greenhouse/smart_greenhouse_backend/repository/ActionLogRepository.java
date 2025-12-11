package org.greenhouse.smart_greenhouse_backend.repository;

import org.greenhouse.smart_greenhouse_backend.model.documents.ActionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionLogRepository extends MongoRepository<ActionLog, String> {

    List<ActionLog> findActionLogsByGreenhouseCode(String code);

    List<ActionLog> findByGreenhouseCodeOrderByTimestampDesc(String greenhouseCode);

    Page<ActionLog> findByGreenhouseCode(String greenhouseCode, Pageable pageable);
}
