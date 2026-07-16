package org.waterwood.waterfunservicecore.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PriorityConverter implements AttributeConverter<Priority, Byte> {

    @Override
    public Byte convertToDatabaseColumn(Priority attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public Priority convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : Priority.fromCode(dbData);
    }
}
