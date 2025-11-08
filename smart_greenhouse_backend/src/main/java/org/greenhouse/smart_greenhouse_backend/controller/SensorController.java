package org.greenhouse.smart_greenhouse_backend.controller;

import org.greenhouse.smart_greenhouse_backend.model.documents.SensorData;
import org.greenhouse.smart_greenhouse_backend.service.sensor.SensorService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/latest")
    public Optional<SensorData> latest() {
        return sensorService.latest();
    }
}