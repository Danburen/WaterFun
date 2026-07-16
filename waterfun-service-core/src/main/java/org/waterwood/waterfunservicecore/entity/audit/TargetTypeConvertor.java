package org.waterwood.waterfunservicecore.entity.audit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TargetTypeConvertor implements AttributeConverter<TargetType, Byte> {
    @Override
    public Byte convertToDatabaseColumn(TargetType attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public TargetType convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : TargetType.fromCode(dbData);
    }
}
