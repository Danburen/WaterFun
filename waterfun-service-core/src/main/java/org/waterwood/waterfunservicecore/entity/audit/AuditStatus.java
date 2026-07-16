package org.waterwood.waterfunservicecore.entity.audit;

import lombok.Getter;

@Getter
public enum AuditStatus {
    PENDING(1),
    APPROVED(2),
    REJECTED(3),
    SUSPECT(4),
    CANCELED(5),;

    private final byte code;
    private AuditStatus(int code) {
        this.code = (byte) code;
    }

    public static AuditStatus fromCode(Byte code) {
        for (AuditStatus status : AuditStatus.values()) {
            if (status.getCode() == code.intValue()) {
                return status;
            }
        }
        throw new IllegalArgumentException("No AuditStatus with code " + code);
    }
}
