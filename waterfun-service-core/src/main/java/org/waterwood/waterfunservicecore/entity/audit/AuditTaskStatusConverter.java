package org.waterwood.waterfunservicecore.entity.audit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AuditTaskStatusConverter implements AttributeConverter<AuditStatus, Byte> {

    @Override
    public Byte convertToDatabaseColumn(AuditStatus auditTaskStatus) {
        return auditTaskStatus == null ? null : auditTaskStatus.getCode();
    }

    @Override
    public AuditStatus convertToEntityAttribute(Byte aShort) {
        return aShort == null ? null : AuditStatus.fromCode(aShort);
    }
}
