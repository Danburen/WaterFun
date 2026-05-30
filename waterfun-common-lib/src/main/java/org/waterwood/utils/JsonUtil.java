package org.waterwood.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public final class JsonUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private JsonUtil() {}
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }


    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null) return null;
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
