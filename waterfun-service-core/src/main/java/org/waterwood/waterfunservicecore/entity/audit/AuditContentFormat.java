package org.waterwood.waterfunservicecore.entity.audit;

import lombok.Getter;

@Getter
public enum AuditContentFormat {
    DEFAULT(0),
    RICH(1),
    IMAGE(2),
    TXT(3);
    private final short value;
    private AuditContentFormat(final int value) {
        this.value = (short) value;
    }

    public static AuditContentFormat fromCode(Short dbData) {
       for (AuditContentFormat format : AuditContentFormat.values()) {
           if (format.getValue() == dbData) {
               return format;
           }
       }
       throw new IllegalArgumentException("Unknown AuditContentFormat: " + dbData);
    }
}
