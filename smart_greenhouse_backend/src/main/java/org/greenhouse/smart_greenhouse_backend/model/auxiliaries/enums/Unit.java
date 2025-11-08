package org.greenhouse.smart_greenhouse_backend.model.auxiliaries.enums;

public enum Unit {
    CELSIUS("°C"),
    FAHRENHEIT("°F"),
    KELVIN("K"),
    PERCENT("%"),
    METER_PER_SECOND("m/s"),
    KILOMETER_PER_HOUR("km/h"),
    MILLIMETER("mm");

    private final String symbol;

    Unit(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}