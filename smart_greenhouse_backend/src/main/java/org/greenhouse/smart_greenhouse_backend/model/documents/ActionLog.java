package org.greenhouse.smart_greenhouse_backend.model.documents;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document("actionLogs")
public class ActionLog {

    private String greenhouseCode;
    private Instant timestamp;
    private String action; // VENT_OPEN, IRRIGATION_ON...
    private String reason; // ruleId vagy "manual"
}