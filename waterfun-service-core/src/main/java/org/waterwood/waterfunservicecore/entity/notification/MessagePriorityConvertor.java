package org.waterwood.waterfunservicecore.entity.notification;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.waterwood.waterfunservicecore.entity.Priority;

@Converter(autoApply = true)
public class MessagePriorityConvertor implements AttributeConverter<Priority, Short> {
    @Override
    public Short convertToDatabaseColumn(Priority attribute) {
        return attribute == null ? 3 : attribute.getValue();
    }

    @Override
    public Priority convertToEntityAttribute(Short dbData) {
        return Priority.fromCode(dbData);
    }
}
