package org.waterwood.waterfunservicecore.entity.audit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ContentFormatConvertor implements AttributeConverter<AuditContentFormat, Short> {

    @Override
    public Short convertToDatabaseColumn(AuditContentFormat auditContentFormat) {
        return auditContentFormat == null ? null : auditContentFormat.getValue();
    }

    @Override
    public AuditContentFormat convertToEntityAttribute(Short aShort) {
        return aShort == null ? null : AuditContentFormat.fromCode(aShort);
    }
}
