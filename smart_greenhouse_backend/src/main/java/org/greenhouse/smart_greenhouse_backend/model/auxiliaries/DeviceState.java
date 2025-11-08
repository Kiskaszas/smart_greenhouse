package org.greenhouse.smart_greenhouse_backend.model.auxiliaries;

import lombok.Data;

@Data
public class DeviceState {
    private boolean irrigationOn;
    private boolean ventOpen;
    private boolean shadeOn;
    private boolean lightOn;
    private boolean humidifierOn;
}