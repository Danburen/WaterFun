package org.waterwood.waterfunservicecore.entity.notification;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.waterwood.waterfunservicecore.entity.Priority;

@Converter
public class MessagePriorityConvertor implements AttributeConverter<Priority, Byte> {
    @Override
    public Byte convertToDatabaseColumn(Priority attribute) {
        return attribute == null ? 3 : attribute.getValue();
    }

    @Override
    public Priority convertToEntityAttribute(Byte dbData) {
        return Priority.fromCode(dbData);
    }
}
