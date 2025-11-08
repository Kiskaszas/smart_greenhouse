package org.greenhouse.smart_greenhouse_backend.model.documents;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Egy vezérlési esemény naplózott adatait tárolja.
 * Például: öntözés indítása, szellőztetés bekapcsolása.
 */
@Data
@Builder
@Document("control_events")
public class ControlEvent {

    private Instant timestamp;    // mikor történt az esemény
    private String greenhouseCode; // Üvegház azonosító kódja
    private String type;         // IRRIGATION, VENTILATION, stb.
    private String action;       // START, STOP, OPEN, CLOSE
    private String reason;       // miért történt (pl. "Manual command", "Rule engine")
    private Integer durationMin; // ha időzített akció, mennyi ideig tart
}