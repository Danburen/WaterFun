package org.waterwood.waterfunservicecore.entity.notification;

import lombok.Getter;

@Getter
public enum BusinessType {
    NONE(0),
    POST(1),
    COMMENT(2),;

    private final short value;
    private BusinessType(final int value) {
        this.value = (short) value;
    }

    public static BusinessType fromValue(final int value) {
        for (final BusinessType type : BusinessType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown business type: " + value);
    }
}
