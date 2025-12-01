package org.greenhouse.smart_greenhouse_backend.exception;

public class PlanNotFoundException extends RuntimeException {

    public PlanNotFoundException(String plantType) {
        super(String.format("Nincs plant profile a megadott plantType-hoz: %s", plantType));
    }
}
