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
        return switch (code) {
            case 1 -> PENDING;
            case 2 -> APPROVED;
            case 3 -> REJECTED;
            default -> throw new IllegalArgumentException("Unknown code " + code);
        };
    }
}
