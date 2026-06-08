package org.waterwood.waterfunservicecore.entity.notification;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MessagePriorityConvertor implements AttributeConverter<NoticePriority, Short> {
    @Override
    public Short convertToDatabaseColumn(NoticePriority attribute) {
        return attribute == null ? 3 : attribute.getValue();
    }

    @Override
    public NoticePriority convertToEntityAttribute(Short dbData) {
        return NoticePriority.fromCode(dbData);
    }
}
