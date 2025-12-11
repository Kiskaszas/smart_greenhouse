package org.greenhouse.smart_greenhouse_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.greenhouse.smart_greenhouse_backend.model.documents.PlantProfile;
import org.greenhouse.smart_greenhouse_backend.repository.PlantProfileRepository;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlantProfileLoader {

    private final PlantProfileRepository plantProfileRepository;
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final Map<String, PlantProfile> cache = new ConcurrentHashMap<>();

    public PlantProfile loadProfile(String plantCodeOrType) {
        if (plantCodeOrType == null) return null;
        String key = plantCodeOrType.toUpperCase();

        return cache.computeIfAbsent(key, k -> {
            // 1) próbáljuk DB-ből
            try {
                if (plantProfileRepository != null) {
                    return plantProfileRepository.findByPlantCode(k)
                            .or(() -> plantProfileRepository.findByPlantType(k))
                            .orElseGet(() -> loadFromClasspath(k));
                }
            } catch (Exception e) {
                log.debug("PlantProfileRepository nem elérhető vagy hiba: {}", e.getMessage());
            }
            // 2) fallback: classpath YAML
            return loadFromClasspath(k);
        });
    }

    private PlantProfile loadFromClasspath(String plantCode) {
        String path = "/plant-profiles/" + plantCode + ".yml";
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                log.warn("Plant profile nem található classpath-on: {}", path);
                return null;
            }
            return yamlMapper.readValue(is, PlantProfile.class);
        } catch (Exception e) {
            log.warn("Hiba plant profile betöltésénél: {} -> {}", path, e.getMessage());
            return null;
        }
    }
}