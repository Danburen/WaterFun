package org.waterwood.waterfunservicecore.entity.post;

import lombok.Getter;

@Getter
public enum PostVisibility {
    PUBLIC(0),
    PRIVATE(1),
    FANS_ONLY(2),;

    private final short code;
    PostVisibility(int code) {
        this.code = (short) code;
    }

    public static PostVisibility fromCode(short code) {
        return switch (code) {
            case 0 -> PUBLIC;
            case 1 -> PRIVATE;
            case 2 -> FANS_ONLY;
            default -> throw new IllegalArgumentException("Invalid PostVisibility code: " + code);
        };
    }
}
