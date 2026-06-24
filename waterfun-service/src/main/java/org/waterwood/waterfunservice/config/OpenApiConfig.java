package org.waterwood.waterfunservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import jakarta.annotation.PostConstruct;
import org.springdoc.core.utils.SpringDocUtils;
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
                ).schema("Long", new Schema<>().type("string").format("int64"));
    }

    @PostConstruct
    public void config() {
        SpringDocUtils.getConfig()
                .replaceWithSchema(Long.class, new StringSchema().format("int64"));
        SpringDocUtils.getConfig()
                .replaceWithSchema(long.class, new StringSchema().format("int64"));
    }
}