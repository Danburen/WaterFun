package org.waterwood.waterfunservicecore.entity;

import lombok.Getter;

@Getter
public enum KeyStatus {
    PENDING_ACTIVATION(0),
    ACTIVE(1),
    DECRYPT_ONLY(2),
    SUSPENDED(3),
    DEACTIVATED(4),
    DESTROYED(5);

    private final byte value;

    KeyStatus(final int value) {
        this.value = (byte) value;
    }

    public static KeyStatus fromValue(final byte value) {
        for (KeyStatus ks : values()) {
            if (ks.value == value) return ks;
        }
        throw new IllegalArgumentException("Unknown key status: " + value);
    }
}
