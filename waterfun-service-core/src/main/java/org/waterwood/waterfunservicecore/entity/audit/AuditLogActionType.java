package org.waterwood.waterfunservicecore.entity.audit;

import lombok.Getter;

@Getter
public enum AuditLogActionType {
    UNKNOWN(0),
    LOGIN(1),
    REGISTER(2),
    CHANGE_PASSWORD(3),;

    private final short value;
    private AuditLogActionType(final int value) {
        this.value = (short) value;
    }

    public static AuditLogActionType fromValue(final int value) {
        for (final AuditLogActionType auditLogActionType : AuditLogActionType.values()) {
            if (auditLogActionType.value == value) {
                return auditLogActionType;
            }
        }
        return UNKNOWN;
    }
}
