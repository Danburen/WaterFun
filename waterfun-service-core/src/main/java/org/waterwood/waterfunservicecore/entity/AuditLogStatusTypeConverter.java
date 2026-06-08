package org.waterwood.waterfunservicecore.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AuditLogStatusTypeConverter implements AttributeConverter<AuditLogStatusType, Short> {
    @Override
    public Short convertToDatabaseColumn(AuditLogStatusType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public AuditLogStatusType convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : AuditLogStatusType.fromValue(dbData);
    }
}
