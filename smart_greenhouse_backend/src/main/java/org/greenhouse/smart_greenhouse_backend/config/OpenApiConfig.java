package org.greenhouse.smart_greenhouse_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Greenhouse API")
                        .version("1.0")
                        .description("Időjárásvezérelt üvegház vezérlő rendszer REST API dokumentáció"));
    }
}