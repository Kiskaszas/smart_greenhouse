package org.greenhouse.smart_greenhouse_backend.model.documents;

import lombok.Data;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.DeviceState;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.Location;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.SensorRef;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Document("greenhouses")
public class Greenhouse {
    @Id
    private String id;

    @Indexed(unique = true)
    private String code;
    private String name;
    private String plantType;
    private boolean active;

    private Location location;
    private List<SensorRef> sensors = new ArrayList<>();
    private DeviceState devices = new DeviceState();

    private String plantProfileId;
    private String planId;

    private Map<String, Instant> lastActionAt = new HashMap<>();
}