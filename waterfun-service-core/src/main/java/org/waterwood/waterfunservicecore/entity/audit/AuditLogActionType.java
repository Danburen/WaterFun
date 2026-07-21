package org.waterwood.waterfunservicecore.entity.audit;

import lombok.Getter;

@Getter
public enum AuditLogActionType {
    UNKNOWN(0),
    LOGIN(1),
    REGISTER(2),
    CHANGE_PASSWORD(3),
    BIND_EMAIL(4),
    UNBIND_EMAIL(5),
    CHANGE_PHONE(6),
    FORGOT_PASSWORD(7),
    CHANGE_EMAIL(8);

    private final byte value;
    AuditLogActionType(final int value) {
        this.value = (byte) value;
    }

    public static AuditLogActionType fromValue(final byte value) {
        for (final AuditLogActionType t : AuditLogActionType.values()) {
            if (t.value == value) {
                return t;
            }
        }
        return UNKNOWN;
    }
}
