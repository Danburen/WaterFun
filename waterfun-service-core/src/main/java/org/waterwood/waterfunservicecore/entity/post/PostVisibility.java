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
        for (PostVisibility postVisibility : PostVisibility.values()) {
            if (postVisibility.code == code) {
                return postVisibility;
            }
        }
        throw new IllegalArgumentException("Unknown PostVisibility code: " + code);
    }
}
