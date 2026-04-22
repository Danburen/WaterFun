package org.waterwood.waterfunservicecore.entity.audit.resource;

import lombok.Getter;

@Getter
public enum AuditResourceType {
    UNKNOWN(0),
    IMAGE(1),
    VIDEO(2),
    AUDIO(3),
    OTHER(4),;

    private final short value;
    AuditResourceType(final int value) {
        this.value = (short) value;
    }

    public static AuditResourceType fromCode(Short value) {
        if(value == null) {
            return UNKNOWN;
        }
        return switch (value) {
            case 1 -> IMAGE;
            case 2 -> VIDEO;
            case 3 -> AUDIO;
            case 4 -> OTHER;
            default -> UNKNOWN;
        };
    }
}
