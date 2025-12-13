package org.greenhouse.smart_greenhouse_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.greenhouse.smart_greenhouse_backend.dto.PlanDto;
import org.greenhouse.smart_greenhouse_backend.dto.WeatherDto;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.DeviceState;
import org.greenhouse.smart_greenhouse_backend.model.auxiliaries.SensorRef;
import org.greenhouse.smart_greenhouse_backend.model.documents.ActionLog;
import org.greenhouse.smart_greenhouse_backend.model.documents.Greenhouse;
import org.greenhouse.smart_greenhouse_backend.model.documents.Plan;
import org.greenhouse.smart_greenhouse_backend.service.actionLog.ActionLogService;
import org.greenhouse.smart_greenhouse_backend.service.greenhouse.GreenhouseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/greenhouses")
@RequiredArgsConstructor
@Tag(name = "Greenhouse API", description = "Üvegházak kezelése és időjárás lekérdezése")
public class GreenhouseController {

    private final GreenhouseService service;

    private final ActionLogService actionLogService;

    @Operation(summary = "Új üvegház létrehozása")
    @ApiResponse(responseCode = "200", description = "Sikeres mentés")
    @PostMapping
    public Greenhouse create(@RequestBody Greenhouse greenhouse) {
        return service.create(greenhouse);
    }

    @Operation(summary = "Üvegházak listázása")
    @ApiResponse(responseCode = "200", description = "Sikeres lekérés")
    @GetMapping
    public List<Greenhouse> list() {
        return service.listAll();
    }

    @Operation(summary = "Üvegház lekérése ID-CODE alapján")
    @ApiResponse(responseCode = "200", description = "Sikeres lekérés")
    @ApiResponse(responseCode = "400", description = "Az üvegház már létezik ezzel a kóddal")
    @ApiResponse(responseCode = "404", description = "Nem található üvegház ezzel a kóddal")
    @GetMapping("/{code}")
    public Greenhouse get(@PathVariable("code") String code) {
        return service.getByCode(code);
    }

    @Operation(summary = "Üvegház adatainak frissítése")
    @ApiResponse(responseCode = "200", description = "Sikeres frissítés")
    @ApiResponse(responseCode = "404", description = "Nem található üvegház ezzel a kóddal")
    @PutMapping("/{code}")
    public Greenhouse update(@PathVariable("code") String code, @RequestBody Greenhouse updatedGreenhouse) {
        return service.updateById(code, updatedGreenhouse);
    }

    @Operation(summary = "Üvegház törlése")
    @ApiResponse(responseCode = "204", description = "Sikeres törlés")
    @ApiResponse(responseCode = "404", description = "Nem található üvegház ezzel az ID-val")
    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable("code") String code) {
        service.deleteByCode(code);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Üvegház actív/inaktívvá tétele.")
    @ApiResponse(responseCode = "200", description = "Sikeres mentés")
    @ApiResponse(responseCode = "404", description = "Nem található üvegház ezzel az ID-val")
    @PostMapping("/{code}/active")
    public Greenhouse setActive(
            @PathVariable("code") String code,
            @RequestParam("active") boolean active
    ) {
        return service.setActive(code, active);
    }

    @Operation(summary = "Szenzoradat beküldése egy üvegházhoz")
    @ApiResponse(responseCode = "200", description = "Sikeres mentés")
    @ApiResponse(responseCode = "404", description = "Nem található üvegház ezzel az ID-val")
    @PostMapping("/{code}/sensors")
    public Greenhouse addSensorData(@PathVariable("code") String code,
                                    @Valid @RequestBody SensorRef sensor
    ) {
        return service.addSensorData(code, sensor);
    }

    @Operation(summary = "Szenzoradat frissítése egy üvegházon")
    @ApiResponse(responseCode = "200", description = "Sikeres mentés")
    @ApiResponse(responseCode = "404", description = "Nem található üvegház ezzel az ID-val")
    @PutMapping("/{code}/{sensorId}")
    public Greenhouse updateSensorData(@PathVariable("code") String code, @Valid @RequestBody SensorRef sensor) {
        return service.updateSensorData(code, sensor);
    }

    @Operation(summary = "Szenzoradat törlése egy üvegháról")
    @ApiResponse(responseCode = "200", description = "Sikeres mentés")
    @ApiResponse(responseCode = "404", description = "Nem található üvegház ezzel az ID-val")
    @DeleteMapping("/{code}/{sensorId}")
    public Greenhouse deleteSensorData(@PathVariable("code") String code, @PathVariable("sensorId") String sensorId) {
        return service.removeSensorData(code, sensorId);
    }

    @Operation(summary = "Eszközök aktuális állapotának lekérése")
    @ApiResponse(responseCode = "200", description = "Sikeres lekérés")
    @ApiResponse(responseCode = "404", description = "Nem található üvegház ezzel az ID-val")
    @GetMapping("/{code}/state")
    public DeviceState getState(@PathVariable("code") String code) {
        return service.getState(code);
    }

    @Operation(summary = "Manuális eszközvezérlés (pl. VENT_OPEN)")
    @ApiResponse(responseCode = "200", description = "Akció sikeresen végrehajtva")
    @ApiResponse(responseCode = "404", description = "Nem található üvegház ezzel az ID-val")
    @PostMapping("/{code}/devices/{action}")
    public DeviceState manualAction(@PathVariable("code") String code, @PathVariable("action") String action) {
        return service.manualAction(code, action);
    }

    @Operation(summary = "Aktuális időjárás lekérése egy üvegházhoz")
    @ApiResponse(responseCode = "200", description = "Sikeres lekérés")
    @ApiResponse(responseCode = "404", description = "Nem található üvegház ezzel az ID-val")
    @GetMapping("/{code}/weather/current")
    public WeatherDto currentWeather(@PathVariable("code") String code) {
        return service.fetchWeatherForGreenhouse(code);
    }

    @Operation(summary = "3 napos időjárás-előrejelzés lekérése egy üvegházhoz")
    @ApiResponse(responseCode = "200", description = "Sikeres lekérés")
    @ApiResponse(responseCode = "404", description = "Nem található üvegház ezzel az kóddal.")
    @GetMapping("/{code}/weather/forecast")
    public List<WeatherDto> forecast(@PathVariable("code") String code) {
        return service.fetchForecastForGreenhouse(code);
    }

    @Operation(summary = "Automatikusan generált terv lekérése egy üvegházhoz")
    @ApiResponse(responseCode = "200", description = "Sikeres lekérés")
    @ApiResponse(responseCode = "404", description = "Nem található üvegház ezzel a kóddal.")
    @GetMapping("/{code}/plan")
    public PlanDto getPlan(@PathVariable("code") String code) {
        Plan plan = service.getLastPlanForGreenhouse(code);
        return PlanDto.builder()
                .validFrom(plan.getValidFrom().toString())
                .validTo(plan.getValidTo().toString())
                .events(plan.getEvents())
                .build();
    }

    @Operation(summary = "Akciónapló lekérése egy üvegházhoz")
    @ApiResponse(responseCode = "200", description = "Sikeres lekérés")
    @ApiResponse(responseCode = "404", description = "Nem található üvegház ezzel a kóddal.")
    @GetMapping("/{code}/logs")
    public List<ActionLog> logs(@PathVariable("code") String code) {
        return service.getLogs(code);
    }

    @Operation(summary = "Adott szimuláció elindítása az üvegházra.")
    @ApiResponse(responseCode = "200", description = "Akció sikeresen végrehajtva")
    @ApiResponse(responseCode = "404", description = "Nem található üvegház ezzel az ID-val")
    @PostMapping("/{code}/simulate")
    public Greenhouse simulateNow(@PathVariable("code") String code) {
        return service.simulateNow(code);
    }

    @Operation(
            summary = "Akciónapló lekérése egy üvegházhoz",
            description = "Visszaadja az adott üvegházhoz tartozó akciónaplót lapozható formában. "
                    + "Az akciók időrendben (timestamp szerint) csökkenő sorrendben érkeznek."
    )
    @ApiResponse(responseCode = "200", description = "Sikeres lekérés, a válaszban Page<ActionLog> objektum található")
    @ApiResponse(responseCode = "404", description = "Nem található üvegház a megadott kóddal")
    @GetMapping("/{code}/actions")
    public ResponseEntity<Page<ActionLog>> getActionLogs(
            @PathVariable("code") String code,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<ActionLog> logs = actionLogService.findByGreenhouseCode(code, pageable);
        return ResponseEntity.ok(logs);
    }
}