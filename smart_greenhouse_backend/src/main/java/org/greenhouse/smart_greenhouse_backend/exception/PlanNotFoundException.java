package org.greenhouse.smart_greenhouse_backend.exception;

public class PlanNotFoundException extends RuntimeException {
    public PlanNotFoundException(String greenhouseCode) {
        super("Nincs terv az üvegházhoz: " + greenhouseCode);
    }
}