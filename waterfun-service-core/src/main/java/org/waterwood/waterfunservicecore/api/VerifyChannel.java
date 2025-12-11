package org.waterwood.waterfunservicecore.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VerifyChannel {
    SMS,
    EMAIL,;

    @JsonValue
    public String getValue() { return name().toLowerCase(); }
    @JsonCreator
    public static VerifyChannel fromValue(String v) {
        return valueOf(v.toUpperCase());
    }
}
