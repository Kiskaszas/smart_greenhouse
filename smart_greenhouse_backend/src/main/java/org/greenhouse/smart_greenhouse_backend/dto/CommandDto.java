package org.greenhouse.smart_greenhouse_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Manuális vezérlési parancs DTO.
 * A felhasználó ezzel küldhet parancsot az üvegház eszközeinek.
 */
@Data
public class CommandDto {

    @NotBlank
    private String greenhouseCode;

    @NotBlank
    private String type;       // IRRIGATION, VENTILATION, stb.

    @NotBlank
    private String action;     // START, STOP, OPEN, CLOSE

    private Integer durationMin; // opcionális, ha időzített akció
}