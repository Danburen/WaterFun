package org.waterwood.waterfunservicecore.entity.audit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RejectTypeConverter implements AttributeConverter<AuditType, Short> {
    @Override
    public Short convertToDatabaseColumn(AuditType attribute) {
        return attribute == null ? null : (short) attribute.getCode();
    }

    @Override
    public AuditType convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : AuditType.fromCode(dbData.intValue());
    }
}
