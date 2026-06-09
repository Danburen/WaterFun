package org.waterwood.waterfunservicecore.entity.audit;

import lombok.Getter;

@Getter
public enum AuditLogStatusType {
    FAIL(0),
    SUCCESS(1),;

    private final short value;
    AuditLogStatusType(final int value) {
        this.value = (short) value;
    }

    public static AuditLogStatusType fromValue(final int value) {
        for (AuditLogStatusType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid AuditLogStatusType value: " + value);
    }
}
