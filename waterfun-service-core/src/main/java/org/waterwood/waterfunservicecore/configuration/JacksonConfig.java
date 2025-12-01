package org.waterwood.waterfunservicecore.configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.time.Instant;

@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        JavaTimeModule module = new JavaTimeModule();
        // Instant → 毫秒时间戳
        module.addSerializer(Instant.class, new JsonSerializer<>() {
            @Override
            public void serialize(Instant value, JsonGenerator gen,
                                  SerializerProvider serializers) throws IOException {
                gen.writeNumber(value.toEpochMilli());   // 只写 13 位毫秒
            }
        });
        return Jackson2ObjectMapperBuilder.json()
                .modules(module)
                .build();
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder
                .featuresToEnable(MapperFeature.USE_STD_BEAN_NAMING)
                .postConfigurer(objectMapper -> {
                    objectMapper.registerModule(new ParameterNamesModule());
                    objectMapper.registerModule(new JavaTimeModule());
                });
    }
} 