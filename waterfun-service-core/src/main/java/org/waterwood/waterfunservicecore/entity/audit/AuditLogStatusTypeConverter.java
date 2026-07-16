package org.waterwood.waterfunservicecore.entity.audit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AuditLogStatusTypeConverter implements AttributeConverter<AuditLogStatusType, Byte> {
    @Override
    public Byte convertToDatabaseColumn(AuditLogStatusType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public AuditLogStatusType convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : AuditLogStatusType.fromValue(dbData);
    }
}
