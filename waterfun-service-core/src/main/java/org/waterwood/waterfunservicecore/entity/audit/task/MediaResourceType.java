package org.waterwood.waterfunservicecore.entity.audit.task;

import lombok.Getter;

import java.util.Locale;

@Getter
public enum MediaResourceType {
    UNKNOWN(0),
    USER_AVATAR(1),;

    private final short code;

    MediaResourceType(final int code) {
        this.code = (short) code;
    }

    public static MediaResourceType fromCode(int code) {
        return switch (code) {
            case 1 -> USER_AVATAR;
            default -> UNKNOWN;
        };
    }

    public String toLowerCase() {
        return name().toLowerCase(Locale.ROOT);
    }
}
