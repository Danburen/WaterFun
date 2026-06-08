package org.waterwood.waterfunservicecore.entity.notification;

import lombok.Getter;

@Getter
public enum NoticePriority {
    EMERGENCY(0),
    HIGH(1),
    MEDIUM(2),
    LOW(3),;

    private final short value;
    private NoticePriority(final int value) {
        this.value = (short) value;
    }

    public static NoticePriority fromCode(final short code) {
        return switch (code) {
            case 0 -> EMERGENCY;
            case 1 -> HIGH;
            case 2 -> MEDIUM;
            case 3 -> LOW;
            default -> NoticePriority.HIGH;
        };
    }
}
