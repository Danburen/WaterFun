package org.waterwood.waterfunservicecore.entity.post;

import lombok.Getter;

public enum PostEditStatus {
    NONE(0),
    PENDING(1),;
    @Getter
    private final short value;
    PostEditStatus(int value) {
        this.value = (short) value;
    }

    public static PostEditStatus fromCode(short code) {
        return switch (code) {
            case 0 -> NONE;
            case 1 -> PENDING;
            default -> throw  new IllegalArgumentException("Invalid PostEditStatus code: " + code);
        };
    }
}
