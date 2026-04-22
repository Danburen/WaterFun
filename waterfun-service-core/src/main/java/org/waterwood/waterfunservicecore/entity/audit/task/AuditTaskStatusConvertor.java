package org.waterwood.waterfunservicecore.entity.audit.task;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;

@Converter(autoApply = true)
public class AuditTaskStatusConvertor implements AttributeConverter<AuditStatus, Short> {

    @Override
    public Short convertToDatabaseColumn(AuditStatus auditTaskStatus) {
        return auditTaskStatus == null ? null : auditTaskStatus.getCode();
    }

    @Override
    public AuditStatus convertToEntityAttribute(Short aShort) {
        return aShort == null ? null : AuditStatus.fromCode(aShort);
    }
}
