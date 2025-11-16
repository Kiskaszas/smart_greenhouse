package org.greenhouse.smart_greenhouse_backend.model.auxiliaries.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ActionType {
    START("START"),
    STOP("STOP"),
    OPEN("OPEN"),
    CLOSE("CLOSE");

    private final String action;

    ActionType(final String action) {this.action = action;}

    @JsonValue
    public String getAction() {return action;}
}
