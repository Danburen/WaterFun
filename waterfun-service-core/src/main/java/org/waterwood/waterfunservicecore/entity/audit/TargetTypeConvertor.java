package org.waterwood.waterfunservicecore.entity.audit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.waterwood.waterfunservicecore.entity.audit.task.MediaResourceType;

@Converter(autoApply = true)
public class TargetTypeConvertor implements AttributeConverter<MediaResourceType, Short> {
    @Override
    public Short convertToDatabaseColumn(MediaResourceType attribute) {
        return attribute == null ? null : (short) attribute.getCode();
    }

    @Override
    public MediaResourceType convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : MediaResourceType.fromCode(dbData.intValue());
    }
}
