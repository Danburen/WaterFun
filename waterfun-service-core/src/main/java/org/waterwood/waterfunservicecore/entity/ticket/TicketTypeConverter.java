package org.waterwood.waterfunservicecore.entity.ticket;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TicketTypeConverter implements AttributeConverter<TicketType, Byte> {

    @Override
    public Byte convertToDatabaseColumn(TicketType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public TicketType convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : TicketType.fromValue(dbData);
    }
}
