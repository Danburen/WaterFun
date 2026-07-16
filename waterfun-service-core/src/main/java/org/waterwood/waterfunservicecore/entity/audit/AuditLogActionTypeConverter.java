package org.waterwood.waterfunservicecore.entity.audit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AuditLogActionTypeConverter implements AttributeConverter<AuditLogActionType, Byte> {
    @Override
    public Byte convertToDatabaseColumn(AuditLogActionType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public AuditLogActionType convertToEntityAttribute(Byte dbData) {
        if (dbData == null) {
            return null;
        }
        return AuditLogActionType.fromValue(dbData);
    }
}
