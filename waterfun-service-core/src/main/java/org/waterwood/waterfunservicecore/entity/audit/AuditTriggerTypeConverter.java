package org.waterwood.waterfunservicecore.entity.audit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AuditTriggerTypeConverter implements AttributeConverter<AuditTriggerType, Short> {


    @Override
    public Short convertToDatabaseColumn(AuditTriggerType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public AuditTriggerType convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : AuditTriggerType.fromValue(dbData);
    }
}
