package org.waterwood.waterfunservicecore.entity.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GenderConverter implements AttributeConverter<Gender, Byte> {

    @Override
    public Byte convertToDatabaseColumn(Gender attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public Gender convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : Gender.fromValue(dbData);
    }
}
