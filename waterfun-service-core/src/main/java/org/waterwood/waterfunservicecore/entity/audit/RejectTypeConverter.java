package org.waterwood.waterfunservicecore.entity.audit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RejectTypeConverter implements AttributeConverter<AuditType, Byte> {
    @Override
    public Byte convertToDatabaseColumn(AuditType attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public AuditType convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : AuditType.fromCode(dbData);
    }
}
