package org.waterwood.waterfunservicecore.entity.resource;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SourceTypeConverter implements AttributeConverter<SourceType, Byte> {

    @Override
    public Byte convertToDatabaseColumn(SourceType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public SourceType convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : SourceType.fromCode(dbData);
    }
}
