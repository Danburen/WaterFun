package org.waterwood.waterfunservicecore.entity.notification;

import lombok.Getter;

@Getter
public enum BusinessType {
    NONE(0),
    POST(1),
    COMMENT(2),
    USER(3),
    TICKET_REPLY(4),;

    private final byte value;
    private BusinessType(final int value) {
        this.value = (byte) value;
    }

    public static BusinessType fromValue(final byte value) {
        for (final BusinessType type : BusinessType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown business type: " + value);
    }
}
