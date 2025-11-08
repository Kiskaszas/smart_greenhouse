package org.greenhouse.smart_greenhouse_backend.model.auxiliaries;

public record Location(
    String city,
    double lat,
    double lon
) {
}
