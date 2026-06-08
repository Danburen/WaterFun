package org.waterwood.waterfunservicecore.entity.post;


import lombok.Getter;

@Getter
public enum CommentStatus {
    DELETED(0),
    NORMAL(1),;

    private final short value;
    CommentStatus(int value) {
        this.value = (short) value;
    }

    public static CommentStatus fromValue(short value) {
        for (CommentStatus status : CommentStatus.values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown post comment status: " + value);
    }
}
