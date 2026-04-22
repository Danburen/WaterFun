package org.waterwood.waterfunservicecore.entity.audit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RejectTypeConvertor implements AttributeConverter<AuditRejectType, Short> {
    @Override
    public Short convertToDatabaseColumn(AuditRejectType attribute) {
        return attribute == null ? null : (short) attribute.getCode();
    }

    @Override
    public AuditRejectType convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : AuditRejectType.fromCode(dbData.intValue());
    }
}
