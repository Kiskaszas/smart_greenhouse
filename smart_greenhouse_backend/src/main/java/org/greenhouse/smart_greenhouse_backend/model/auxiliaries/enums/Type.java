package org.greenhouse.smart_greenhouse_backend.model.auxiliaries.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Type {
    TEMPERATURE("temperature"),
    WIND_SPEED("windSpeed"),
    PRECIPITATION_MM("precipitationMm"),
    HUMIDITY_PCT("humidityPct"),
    SOILMOISTURE_PTC("soilMoisturePct");

    private final String type;

    Type(String type) {
        this.type = type;
    }

    @JsonValue
    public String getType() {
        return type;
    }
}
