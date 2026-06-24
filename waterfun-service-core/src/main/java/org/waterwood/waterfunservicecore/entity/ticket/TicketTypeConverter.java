package org.waterwood.waterfunservicecore.entity.ticket;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TicketTypeConverter implements AttributeConverter<TicketType, Short> {

    @Override
    public Short convertToDatabaseColumn(TicketType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public TicketType convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : TicketType.fromValue(dbData);
    }
}
