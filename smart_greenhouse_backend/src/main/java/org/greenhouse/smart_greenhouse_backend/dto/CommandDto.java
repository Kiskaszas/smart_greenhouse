package org.greenhouse.smart_greenhouse_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.enums.ActionType;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.enums.CommandType;

/**
 * Manuális vezérlési parancs DTO.
 * A felhasználó ezzel küldhet parancsot az üvegház eszközeinek.
 */
@Data
public class CommandDto {

    @NotBlank
    private String greenhouseCode;

    @NotBlank
    private CommandType type;       // IRRIGATION, VENTILATION, stb.

    @NotBlank
    private ActionType action;     // START, STOP, OPEN, CLOSE

    private Integer durationMin; // opcionális, ha időzített akció
}