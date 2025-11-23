package org.greenhouse.smart_greenhouse_backend.exception;

public class PlanNotFoundxception extends RuntimeException {
    public PlanNotFoundxception(String plantType) {
        super(String.format("Nincs plant profile a megadott plantType-hoz: %s", plantType));
    }
}
