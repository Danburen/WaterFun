package org.waterwood.waterfunservicecore.entity.audit;

import lombok.Getter;

@Getter
public enum AuditStatus {
    PENDING(1),
    APPROVED(2),
    REJECTED(3),;

    private final short code;
    private AuditStatus(int code) {
        this.code = (short) code;
    }

    public static AuditStatus fromCode(Short code) {
        if (code == null) {
            return PENDING;
        }
        return switch (code) {
            case 2 -> APPROVED;
            case 3 -> REJECTED;
            default -> PENDING;
        };
    }
}
