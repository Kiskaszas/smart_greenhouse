package org.greenhouse.smart_greenhouse_backend.model.auxiliaries.enums;

public enum Unit {
    CELSIUS("°C"),
    PERCENT("%"),
    KILO_METER_PER_HOUR("km/h"),
    MILLIMETER("mm");

    private final String symbol;

    Unit(String symbol) {
        this.symbol = symbol;
    }

}