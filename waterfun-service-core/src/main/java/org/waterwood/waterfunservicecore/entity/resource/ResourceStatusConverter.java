package org.waterwood.waterfunservicecore.entity.resource;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ResourceStatusConverter implements AttributeConverter<ResourceStatus, Short> {
    @Override
    public Short convertToDatabaseColumn(ResourceStatus attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public ResourceStatus convertToEntityAttribute(Short dbData) {
        return dbData != null ? ResourceStatus.valueOf(dbData) : null;
    }
}
