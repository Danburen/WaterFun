package org.waterwood.waterfunservicecore.entity.ticket;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TicketRejectTypeConverter implements AttributeConverter<TicketRejectType, Byte> {

    @Override
    public Byte convertToDatabaseColumn(TicketRejectType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public TicketRejectType convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : TicketRejectType.fromValue(dbData);
    }
}
