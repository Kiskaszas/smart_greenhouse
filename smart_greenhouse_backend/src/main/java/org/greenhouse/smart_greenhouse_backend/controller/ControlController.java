package org.greenhouse.smart_greenhouse_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.greenhouse.smart_greenhouse_backend.dto.CommandDto;
import org.greenhouse.smart_greenhouse_backend.dto.ControlStateDto;
import org.greenhouse.smart_greenhouse_backend.model.documents.ControlEvent;
import org.greenhouse.smart_greenhouse_backend.service.control.ControlService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/control")
@RequiredArgsConstructor
@Tag(name = "Control API", description = "Eszközök vezérlése és vezérlési események naplózása")
public class ControlController {

    private final ControlService service;

    @Operation(summary = "Manuális vezérlési parancs küldése")
    @ApiResponse(responseCode = "200", description = "Parancs sikeresen végrehajtva és naplózva")
    @PostMapping("/command")
    public void command(@RequestBody final CommandDto command) {
        service.manualCommand(
                command.getGreenhouseCode(),
                command.getType(),
                command.getAction(),
                command.getDurationMin()
        );
    }

    @Operation(summary = "Összes vezérlési esemény lekérése")
    @ApiResponse(responseCode = "200", description = "Sikeres lekérés")
    @GetMapping("/events")
    public List<ControlEvent> getAllEvents() {
        return service.getAllEvents();
    }

    @Operation(summary = "Egy vezérlési esemény lekérése ID alapján")
    @ApiResponse(responseCode = "200", description = "Sikeres lekérés")
    @ApiResponse(responseCode = "404", description = "Nem található esemény ezzel az ID-val")
    @GetMapping("/events/{id}")
    public ControlEvent getEvent(@PathVariable("id") String id) {
        return service.getEventById(id);
    }

    @Operation(summary = "Aktuális vezérlési konfiguráció lekérése a greenhousehoz")
    @ApiResponse(responseCode = "200", description = "Sikeres lekérés")
    @GetMapping("/state/greenhouse/{greenhouseCode}")
    public ControlStateDto getControlState(@PathVariable("greenhouseCode") String greenhouseCode) {
        return service.getControlStateByGreenhouseCode(greenhouseCode);
    }

    @Operation(summary = "Vezérlési esemény lekérdezése az üvegház kódja alapján")
    @ApiResponse(responseCode = "200", description = "Sikeres lekérés")
    @ApiResponse(responseCode = "404", description = "Nem található üvegházi esemény ezzel az kóddal")
    @GetMapping("/events/greenhouse/{code}")
    public List<ControlEvent> getAllEventsByGreenhouse(@PathVariable("code") String code){
        return service.getAllEventsForGreenhouse(code);
    }
}