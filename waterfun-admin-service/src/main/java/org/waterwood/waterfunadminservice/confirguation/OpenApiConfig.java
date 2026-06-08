package org.waterwood.waterfunadminservice.confirguation;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSchemas("Instant", new Schema<>()
                                .type("string")
                                .format("date-time")
                                .example("2026-06-05T00:00:00Z")
                        )
                );
    }
}