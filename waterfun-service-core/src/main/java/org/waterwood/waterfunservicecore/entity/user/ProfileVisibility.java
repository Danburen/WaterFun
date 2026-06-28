package org.waterwood.waterfunservicecore.entity.user;

import lombok.Getter;

@Getter
public enum ProfileVisibility {
    PUBLIC(0),
    FOLLOWERS(1),
    PRIVATE(2),;

    private final short value;
    ProfileVisibility(final int value) {
        this.value = (short) value;
    }

    public static ProfileVisibility fromValue(Short value) {
        for (ProfileVisibility v : ProfileVisibility.values()) {
            if (v.value == value) {
                return v;
            }
        }
        throw new IllegalArgumentException("Can't find ProfileVisibility of " + value);
    }
}
