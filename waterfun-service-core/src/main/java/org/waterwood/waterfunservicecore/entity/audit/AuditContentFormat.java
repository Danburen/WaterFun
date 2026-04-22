package org.waterwood.waterfunservicecore.entity.audit;

import lombok.Getter;

@Getter
public enum AuditContentFormat {
    DEFAULT(0),
    PLAINTEXT(1),
    HTML(2),
    MARKDOWN(3),;
    private final short value;
    private AuditContentFormat(final int value) {
        this.value = (short) value;
    }

    public static AuditContentFormat fromCode(Short dbData) {
        if (dbData == null) {
            return PLAINTEXT;
        }
        return switch (dbData) {
            case 0 -> DEFAULT;
            case 1 -> PLAINTEXT;
            case 2 -> HTML;
            case 3 -> MARKDOWN;
            default -> DEFAULT;
        };
    }
}
