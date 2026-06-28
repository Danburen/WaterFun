package org.waterwood.waterfunservicecore.entity.user;

import lombok.Getter;

@Getter
public enum ContentPermission {
    ALL(0),
    FOLLOWERS(1),
    NONE(2),;

    private final short value;
    ContentPermission(final int value) {
        this.value = (short) value;
    }

    public static ContentPermission fromValue(Short value) {
        for (ContentPermission v : ContentPermission.values()) {
            if (v.value == value) {
                return v;
            }
        }
        throw new IllegalArgumentException("Can't find ContentPermission of " + value);
    }
}
