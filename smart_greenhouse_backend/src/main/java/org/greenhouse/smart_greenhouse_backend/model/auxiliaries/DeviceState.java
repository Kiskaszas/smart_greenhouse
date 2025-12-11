package org.greenhouse.smart_greenhouse_backend.model.auxiliaries;

import lombok.Data;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
public class DeviceState {
    private boolean irrigationOn;
    private boolean ventOpen;
    private boolean shadeOn;
    private boolean lightOn;
    private boolean humidifierOn;
    private Map<String, Instant> lastManualActionAt = new HashMap<>();
}