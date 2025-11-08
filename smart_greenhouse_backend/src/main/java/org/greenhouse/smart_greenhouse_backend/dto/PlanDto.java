package org.greenhouse.smart_greenhouse_backend.dto;

import lombok.Builder;
import lombok.Data;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.PlannedEvent;

import java.util.List;

/**
 * A terv DTO-ja, amelyet a kliens felé adunk vissza.
 * Csak a szükséges mezőket tartalmazza.
 */
@Data
@Builder
public class PlanDto {
    private String validFrom;
    private String validTo;
    private List<PlannedEvent> events;
}