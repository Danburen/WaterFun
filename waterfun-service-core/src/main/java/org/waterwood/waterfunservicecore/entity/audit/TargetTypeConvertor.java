package org.waterwood.waterfunservicecore.entity.audit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.waterwood.waterfunservicecore.entity.audit.task.TargetType;

@Converter(autoApply = true)
public class TargetTypeConvertor implements AttributeConverter<TargetType, Short> {
    @Override
    public Short convertToDatabaseColumn(TargetType attribute) {
        return attribute == null ? null : (short) attribute.getCode();
    }

    @Override
    public TargetType convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : TargetType.fromCode(dbData.intValue());
    }
}
