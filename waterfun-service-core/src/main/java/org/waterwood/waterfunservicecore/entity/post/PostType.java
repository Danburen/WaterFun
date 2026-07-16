package org.waterwood.waterfunservicecore.entity.post;

import lombok.Getter;

@Getter
public enum PostType {
    COMMON(0),
    NOTICE(1),;

    private final byte value;
    private PostType(int value) {
        this.value = (byte) value;
    }

    public static PostType fromValue(Byte value) {
        for (PostType postType : PostType.values()) {
            if (postType.value == value) {
                return postType;
            }
        }
        throw new IllegalArgumentException("Unknown Post type value: " + value);
    }
}
