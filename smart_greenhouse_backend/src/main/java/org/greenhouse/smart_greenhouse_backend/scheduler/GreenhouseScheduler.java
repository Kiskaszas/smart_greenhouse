package org.greenhouse.smart_greenhouse_backend.scheduler;

import lombok.RequiredArgsConstructor;
import org.greenhouse.smart_greenhouse_backend.service.greenhouse.GreenhouseService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 30 percenként lefutó rutin, amely minden üvegházhoz
 * lekéri az aktuális állapotot és időjárást, majd snapshotot készít.
 */
@Component
@RequiredArgsConstructor
public class GreenhouseScheduler {

    private final GreenhouseService greenhouseService;

    @Scheduled(cron = "0/1 * * * * *") // 30 percenként
    public void pollAllGreenhouses() {
        greenhouseService.pollAllGreenhouses();
    }
}