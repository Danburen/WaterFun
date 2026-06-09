package org.waterwood.waterfunservicecore.entity.audit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AuditLogActionTypeConverter implements AttributeConverter<AuditLogActionType, Short> {
    @Override
    public Short convertToDatabaseColumn(AuditLogActionType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public AuditLogActionType convertToEntityAttribute(Short dbData) {
        if (dbData == null) {
            return null;
        }
        return AuditLogActionType.fromValue(dbData);
    }
}
