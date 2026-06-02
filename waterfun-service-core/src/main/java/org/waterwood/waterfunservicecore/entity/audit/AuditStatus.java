package org.waterwood.waterfunservicecore.entity.audit;

import lombok.Getter;

@Getter
public enum AuditStatus {
    PENDING(1),
    APPROVED(2),
    REJECTED(3),
    SUSPECT(4);

    private final short code;
    private AuditStatus(int code) {
        this.code = (short) code;
    }

    public static AuditStatus fromCode(Short code) {
        for (AuditStatus status : AuditStatus.values()) {
            if (status.getCode() == code.intValue()) {
                return status;
            }
        }
        throw new IllegalArgumentException("No AuditStatus with code " + code);
    }
}
