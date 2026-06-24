package org.waterwood.waterfunservicecore.entity.ticket;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TicketAuditStatusConverter implements AttributeConverter<TicketAuditStatus, Short> {

    @Override
    public Short convertToDatabaseColumn(TicketAuditStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public TicketAuditStatus convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : TicketAuditStatus.fromValue(dbData);
    }
}
