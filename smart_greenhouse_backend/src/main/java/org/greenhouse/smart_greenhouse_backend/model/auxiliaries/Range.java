package org.greenhouse.smart_greenhouse_backend.model.auxiliaries;

/**
 * Egy értéktartomány (minimum és maximum).
 */
public record Range(
    Double min,
    Double max
) {
}