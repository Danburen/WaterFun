package org.waterwood.waterfunservicecore.entity.audit;

import lombok.Getter;

@Getter
public enum AuditType {
    VIOLATION_OF_GUIDELINES(1),
    INAPPROPRIATE_CONTENT(2),
    ADVERTISEMENT(3),
    VIOLENCE(4),
    SENSITIVE(5),
    CASCADE(98),
    OTHER(99);
    private final short code;
    AuditType(int code) {
        this.code = (short) code;
    }

    public static AuditType fromCode(int code) {
        for (AuditType type : AuditType.values()) {
            if (type.code == code) {
                    return type;
            }
        }
        return OTHER;
    }
}
