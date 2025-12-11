package org.waterwood.waterfunservicecore.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VerifyScene {
    LOGIN,
    REGISTER,
    SET_PASSWORD,
    RESET_PASSWORD,
    CHANGE_EMAIL,
    CHANGE_PHONE,
    ACTIVATE_EMAIL,
    VERIFY,
    BIND_EMAIL;
    @JsonValue
    public String getValue() { return name().toLowerCase(); }
    @JsonCreator
    public static VerifyScene fromValue(String v) {
        return valueOf(v.toUpperCase());
    }

    public static boolean isPublicScene(VerifyScene scene){
        return scene == VerifyScene.LOGIN || scene == VerifyScene.REGISTER;
    }

    public static boolean isAfterAuthorized(VerifyScene scene) {
        return scene == RESET_PASSWORD || scene == SET_PASSWORD
                || scene == CHANGE_EMAIL || scene == CHANGE_PHONE;
    }
}
