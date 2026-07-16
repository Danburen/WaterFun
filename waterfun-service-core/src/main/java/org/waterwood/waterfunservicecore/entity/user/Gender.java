package org.waterwood.waterfunservicecore.entity.user;

import lombok.Getter;

@Getter
public enum Gender {
    MALE(0),
    FEMALE(1),
    OTHER(2),
    UNKNOWN(3);

    private final byte value;

    Gender(final int value) {
        this.value = (byte) value;
    }

    public static Gender fromValue(final byte value) {
        for (Gender g : values()) {
            if (g.value == value) return g;
        }
        throw new IllegalArgumentException("Unknown gender: " + value);
    }
}
