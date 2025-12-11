package org.greenhouse.smart_greenhouse_backend.service.actionLog;

import lombok.RequiredArgsConstructor;
import org.greenhouse.smart_greenhouse_backend.model.documents.ActionLog;
import org.greenhouse.smart_greenhouse_backend.repository.ActionLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActionLogServiceImpl implements ActionLogService {
    private final ActionLogRepository actionLogRepository;

    @Override
    public Page<ActionLog> findByGreenhouseCode(String greenhouseCode, Pageable pageable) {
        return actionLogRepository.findByGreenhouseCode(greenhouseCode, pageable);
    }
}
