package org.greenhouse.smart_greenhouse_backend.exception;

public class PlanNotFoundForGreenhouseException extends RuntimeException {
    public PlanNotFoundForGreenhouseException(String greenhouseCode) {
        super(String.format("Nincs terv az üvegházhoz: %s", greenhouseCode));
    }
}