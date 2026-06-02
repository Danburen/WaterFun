package org.waterwood.waterfunservicecore.entity.post;

import lombok.Getter;

@Getter
public enum PostEditStatus {
    NONE(0),
    PENDING(1),;
    private final short value;
    PostEditStatus(int value) {
        this.value = (short) value;
    }

    public static PostEditStatus fromCode(short code) {
        for (PostEditStatus status : PostEditStatus.values()) {
            if (status.value == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid PostEditStatus code: " + code);
    }
}
