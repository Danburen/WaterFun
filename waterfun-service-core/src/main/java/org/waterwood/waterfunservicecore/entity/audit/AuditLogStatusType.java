package org.waterwood.waterfunservicecore.entity.audit;

import lombok.Getter;

@Getter
public enum AuditLogStatusType {
    FAIL(0),
    SUCCESS(1),;

    private final byte value;
    AuditLogStatusType(final int value) {
        this.value = (byte) value;
    }

    public static AuditLogStatusType fromValue(final byte value) {
        for (AuditLogStatusType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid AuditLogStatusType value: " + value);
    }
}
