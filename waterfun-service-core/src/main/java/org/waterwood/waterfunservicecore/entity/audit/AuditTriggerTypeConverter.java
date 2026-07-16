package org.waterwood.waterfunservicecore.entity.audit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AuditTriggerTypeConverter implements AttributeConverter<AuditTriggerType, Byte> {


    @Override
    public Byte convertToDatabaseColumn(AuditTriggerType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public AuditTriggerType convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : AuditTriggerType.fromValue(dbData);
    }
}
