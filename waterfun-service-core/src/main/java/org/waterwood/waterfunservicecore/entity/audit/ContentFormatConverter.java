package org.waterwood.waterfunservicecore.entity.audit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ContentFormatConverter implements AttributeConverter<AuditContentFormat, Byte> {

    @Override
    public Byte convertToDatabaseColumn(AuditContentFormat auditContentFormat) {
        return auditContentFormat == null ? null : auditContentFormat.getValue();
    }

    @Override
    public AuditContentFormat convertToEntityAttribute(Byte aShort) {
        return aShort == null ? null : AuditContentFormat.fromCode(aShort);
    }
}
