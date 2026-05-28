package org.waterwood.waterfunservicecore.entity.resource;

import lombok.Getter;

public enum SourceType {
    SYSTEM(0),
    CONTENT_ATTACHED(1),
    USER_UPLOADED(2),;

    @Getter
    private final short value;

    SourceType(final int value) {
        this.value = (short) value;
    }

    public static SourceType fromCode(final short code) {
        return switch (code) {
            case 0 -> SYSTEM;
            case 1 -> CONTENT_ATTACHED;
            case 2 -> USER_UPLOADED;
            default -> throw new IllegalArgumentException("Invalid SourceType code: " + code);
        };
    }
}
