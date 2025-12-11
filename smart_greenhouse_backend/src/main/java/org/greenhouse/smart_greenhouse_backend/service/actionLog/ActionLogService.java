package org.greenhouse.smart_greenhouse_backend.service.actionLog;

import org.greenhouse.smart_greenhouse_backend.model.documents.ActionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ActionLogService {

    Page<ActionLog> findByGreenhouseCode(String greenhouseCode, Pageable pageable);
}
