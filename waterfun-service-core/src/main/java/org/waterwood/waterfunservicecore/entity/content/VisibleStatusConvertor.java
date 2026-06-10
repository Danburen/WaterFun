package org.waterwood.waterfunservicecore.entity.content;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class VisibleStatusConvertor implements AttributeConverter<VisibleStatus, Short> {

    @Override
    public Short convertToDatabaseColumn(VisibleStatus attribute) {
        return attribute == null ? 1 : attribute.getCode();
    }

    @Override
    public VisibleStatus convertToEntityAttribute(Short dbData) {
        return VisibleStatus.fromCode(dbData);
    }
}
