package org.greenhouse.smart_greenhouse_backend.exception;

public class GreenhouseIsExistByCodeException extends RuntimeException {
    public GreenhouseIsExistByCodeException(String message) {
        super(message);
    }
}
