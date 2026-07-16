package org.waterwood.waterfunservicecore.entity.user;

import lombok.Getter;

@Getter
public enum ContentPermission {
    ALL(0),
    FOLLOWERS(1),
    NONE(2),;

    private final byte value;
    ContentPermission(final int value) {
        this.value = (byte) value;
    }

    public static ContentPermission fromValue(Byte value) {
        for (ContentPermission v : ContentPermission.values()) {
            if (v.value == value) {
                return v;
            }
        }
        throw new IllegalArgumentException("Can't find ContentPermission of " + value);
    }
}
