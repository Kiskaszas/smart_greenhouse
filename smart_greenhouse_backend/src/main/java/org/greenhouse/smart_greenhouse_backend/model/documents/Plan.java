package org.greenhouse.smart_greenhouse_backend.model.documents;

import lombok.*;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.PlannedEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document("plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {
    @Id
    private String id;

    private String greenhouseCode;
    private Instant validFrom;
    private Instant validTo;
    private boolean active;

    @Builder.Default
    private List<PlannedEvent> events = new ArrayList<>();
}