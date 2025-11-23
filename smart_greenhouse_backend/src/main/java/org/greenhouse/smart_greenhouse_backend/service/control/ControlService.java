package org.greenhouse.smart_greenhouse_backend.service.control;

import org.greenhouse.smart_greenhouse_backend.dto.ControlStateDto;
import org.greenhouse.smart_greenhouse_backend.dto.WeatherDto;
import org.greenhouse.smart_greenhouse_backend.exception.ControlNotFoudException;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.enums.ActionType;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.enums.CommandType;
import org.greenhouse.smart_greenhouse_backend.model.documents.ControlEvent;

import java.util.List;

public interface ControlService {

    void evaluateAndControl(
            final String greenhouseCode,
            final WeatherDto weather,
            final Double latestSoilMoisture
    );

    void manualCommand(
            final String greehouseCode,
            final CommandType type,
            final ActionType action,
            final Integer durationMin
    );

    /**
     * Összes vezérlési esemény lekérése.
     */
    List<ControlEvent> getAllEvents();

    /**
     * Egy vezérlési esemény lekérése ID alapján.
     */
    ControlEvent getEventById(String id);

    /**
     * Aktuális vezérlési konfiguráció visszaadása DTO-ban a greenahouse kódja által.
     */
    ControlStateDto getControlStateByGreenhouseCode(final String greenhouseCode) throws ControlNotFoudException;

    /**
     * Összes vezérlési esemény lekérdezése az adott green housera a dedikált kódja alapján.
     *
     * @param greenhouseCode az üvegház azonosítója.
     */
    List<ControlEvent> getAllEventsForGreenhouse(String greenhouseCode);
}
