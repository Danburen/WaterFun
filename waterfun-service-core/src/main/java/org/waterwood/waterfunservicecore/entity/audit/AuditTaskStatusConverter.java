package org.waterwood.waterfunservicecore.entity.audit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AuditTaskStatusConverter implements AttributeConverter<AuditStatus, Short> {

    @Override
    public Short convertToDatabaseColumn(AuditStatus auditTaskStatus) {
        return auditTaskStatus == null ? null : auditTaskStatus.getCode();
    }

    @Override
    public AuditStatus convertToEntityAttribute(Short aShort) {
        return aShort == null ? null : AuditStatus.fromCode(aShort);
    }
}
