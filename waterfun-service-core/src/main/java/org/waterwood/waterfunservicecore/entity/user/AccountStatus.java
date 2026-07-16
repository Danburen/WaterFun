package org.waterwood.waterfunservicecore.entity.user;

import lombok.Getter;

@Getter
public enum AccountStatus {
    ACTIVE(0),
    SUSPENDED(1),
    DEACTIVATED(2);

    private final byte value;

    AccountStatus(final int value) {
        this.value = (byte) value;
    }

    public static AccountStatus fromValue(final byte value) {
        for (AccountStatus s : values()) {
            if (s.value == value) return s;
        }
        throw new IllegalArgumentException("Unknown account status: " + value);
    }
}
