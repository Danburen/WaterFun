package org.waterwood.waterfunservicecore.entity.audit;

import lombok.Getter;

@Getter
public enum AuditTriggerType {
    UNKNOWN(0),
    USER_SUBMIT(1),
    SYSTEM_DETECTED(2),

    USER_REPORT(3),
    USER_SUGGESTION(4),
    USER_FEEDBACK(5),
    USER_APPEAL(6);

    private final short value;

    AuditTriggerType(final int value) {
        this.value = (short) value;
    }

    public static AuditTriggerType fromValue(final int value) {
        for(final AuditTriggerType type : values()) {
            if(type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown AuditTriggerType value: " + value);
    }
}
