package org.waterwood.waterfunservicecore.entity.security;

import lombok.Getter;
import org.waterwood.waterfunservicecore.entity.audit.AuditType;

@Getter
public enum BanReasonType {
    UNSPECIFIED(0, null),
    VIOLATION_OF_GUIDELINES(1, "reject.reason.violation_of_guidelines"),
    INAPPROPRIATE_CONTENT(2, "reject.reason.inappropriate_content"),
    ADVERTISEMENT(3, "reject.reason.advertisement"),
    VIOLENCE(4, "reject.reason.violence"),
    SENSITIVE(5, "reject.reason.sensitive"),
    CHEATING(6, "ban.reason.cheating"),
    IMPERSONATION(7, "ban.reason.impersonation"), // 新增
    PRIVACY(8, "ban.reason.privacy"),
    TROLLING(9, "ban.reason.trolling"),
    OTHER(99, "reject.reason.other");

    private final short value;
    private final String messageKey;

    public AuditType toAuditType() {
        return switch (this) {
            case UNSPECIFIED, CHEATING, IMPERSONATION, PRIVACY, TROLLING -> AuditType.OTHER;
            case VIOLATION_OF_GUIDELINES -> AuditType.VIOLATION_OF_GUIDELINES;
            case INAPPROPRIATE_CONTENT -> AuditType.INAPPROPRIATE_CONTENT;
            case ADVERTISEMENT -> AuditType.ADVERTISEMENT;
            case VIOLENCE -> AuditType.VIOLENCE;
            case SENSITIVE -> AuditType.SENSITIVE;
            case OTHER -> AuditType.OTHER;
        };
    }
    private BanReasonType(final int value, final String messageKey) {
        this.value = (short) value;
        this.messageKey = messageKey;
    }

    public static BanReasonType fromValue(final short code) {
        for (final BanReasonType b : BanReasonType.values()) {
            if (b.value == code) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unknown BanReasonType code: " + code);
    }
}
