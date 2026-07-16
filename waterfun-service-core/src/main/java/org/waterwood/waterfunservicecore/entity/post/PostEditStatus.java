package org.waterwood.waterfunservicecore.entity.post;

import lombok.Getter;

@Getter
public enum PostEditStatus {
    NONE(0),
    PENDING(1),
    REJECTED(2);
    private final byte value;
    PostEditStatus(int value) {
        this.value = (byte) value;
    }

    public static PostEditStatus fromCode(Byte code) {
        for (PostEditStatus status : PostEditStatus.values()) {
            if (status.value == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid PostEditStatus code: " + code);
    }
}
