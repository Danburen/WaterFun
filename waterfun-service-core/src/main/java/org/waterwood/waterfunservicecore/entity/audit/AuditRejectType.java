package org.waterwood.waterfunservicecore.entity.audit;

import lombok.Getter;

@Getter
public enum AuditRejectType {
    VIOLATION_OF_GUIDELINES(1),
    INAPPROPRIATE_CONTENT(2),
    ADVERTISEMENT(3),
    VIOLENCE(4),
    SENSITIVE(5),
    OTHER(99);
    private final short code;
    AuditRejectType(int code) {
        this.code = (short) code;
    }

    public static AuditRejectType fromCode(int code) {
        for (AuditRejectType type : AuditRejectType.values()) {
            if (type.code == code) {
                    return type;
            }
        }
        return OTHER;
    }
}
