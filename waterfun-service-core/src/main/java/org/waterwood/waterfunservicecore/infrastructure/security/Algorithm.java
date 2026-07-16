package org.waterwood.waterfunservicecore.infrastructure.security;

import lombok.Getter;

@Getter
public enum Algorithm {
    AES(0),
    SM4(1);

    private final byte value;

    Algorithm(final int value) {
        this.value = (byte) value;
    }

    public static Algorithm fromValue(final byte value) {
        for (Algorithm a : values()) {
            if (a.value == value) return a;
        }
        return AES;
    }
}
