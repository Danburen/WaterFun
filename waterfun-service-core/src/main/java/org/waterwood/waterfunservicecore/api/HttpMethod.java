package org.waterwood.waterfunservicecore.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum HttpMethod {
    GET, POST, PUT, DELETE, PATCH;
    @JsonValue
    public String getValue() { return name().toLowerCase(); }
    @JsonCreator
    public static HttpMethod fromValue(String v) {
        return valueOf(v.toUpperCase());
    }
}
