package org.greenhouse.smart_greenhouse_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.RequiredArgsConstructor;
import org.greenhouse.smart_greenhouse_backend.model.documents.PlantProfile;
import org.greenhouse.smart_greenhouse_backend.repository.PlantProfileRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class PlantProfileLoader {

    private final PlantProfileRepository repo;
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @EventListener(ApplicationReadyEvent.class)
    public void loadProfiles() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        for (Resource res : resolver.getResources("classpath:plant-profiles/*.yml")) {
            PlantProfile profile = yamlMapper.readValue(res.getInputStream(), PlantProfile.class);

            repo.findByPlantCode(profile.getPlantCode())
                    .ifPresent(existing -> profile.setId(existing.getId()));

            repo.save(profile);
        }
    }
}