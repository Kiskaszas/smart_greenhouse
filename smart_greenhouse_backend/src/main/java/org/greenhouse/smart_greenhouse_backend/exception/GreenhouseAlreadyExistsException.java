package org.greenhouse.smart_greenhouse_backend.exception;

public class GreenhouseAlreadyExistsException extends RuntimeException {

    public GreenhouseAlreadyExistsException(String message) {
        super(message);
    }
}
