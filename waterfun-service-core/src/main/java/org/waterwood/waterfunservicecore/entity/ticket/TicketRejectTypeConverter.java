package org.waterwood.waterfunservicecore.entity.ticket;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TicketRejectTypeConverter implements AttributeConverter<TicketRejectType, Short> {

    @Override
    public Short convertToDatabaseColumn(TicketRejectType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public TicketRejectType convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : TicketRejectType.fromValue(dbData);
    }
}
