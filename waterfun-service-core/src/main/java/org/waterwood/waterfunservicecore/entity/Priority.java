package org.waterwood.waterfunservicecore.entity;

import lombok.Getter;

@Getter
public enum Priority {
    EMERGENCY(0),
    HIGH(1),
    MEDIUM(2),
    LOW(3),;

    private final byte value;
    private Priority(final int value) {
        this.value = (byte) value;
    }

    public static Priority fromCode(final byte code) {
        return switch (code) {
            case 0 -> EMERGENCY;
            case 1 -> HIGH;
            case 2 -> MEDIUM;
            case 3 -> LOW;
            default -> Priority.HIGH;
        };
    }
}
