package org.greenhouse.smart_greenhouse_backend.exception;

public class ControlNotFoudException extends RuntimeException {
    public ControlNotFoudException(String greenhouseCode) {
        super(String.format("Control nem található a következő üvegházhoz: %s", greenhouseCode));
    }
}
