package org.waterwood.waterfunservicecore.entity.notification;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MessagePriorityConvertor implements AttributeConverter<MessagePriority, Short> {
    @Override
    public Short convertToDatabaseColumn(MessagePriority attribute) {
        return attribute == null ? 3 : attribute.getValue();
    }

    @Override
    public MessagePriority convertToEntityAttribute(Short dbData) {
        return MessagePriority.fromCode(dbData);
    }
}
