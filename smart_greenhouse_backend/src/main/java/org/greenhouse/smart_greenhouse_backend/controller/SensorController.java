package org.greenhouse.smart_greenhouse_backend.controller;

import lombok.RequiredArgsConstructor;
import org.greenhouse.smart_greenhouse_backend.model.documents.SensorData;
import org.greenhouse.smart_greenhouse_backend.service.sensor.SensorService;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
public class SensorController {

    private final SensorService sensorService;

    @PostMapping
    public SensorData ingest(@RequestBody SensorData data) {
        return sensorService.ingest(data);
    }

    @PutMapping("/{sensorCode}")
    public void update(@PathVariable("sensorCode") String sensorCode, @RequestBody SensorData sensor) {
        sensorService.updateByCode(sensorCode, sensor);
    }

    @GetMapping("/latest")
    public Optional<SensorData> latest() {
        return sensorService.latest();
    }
}