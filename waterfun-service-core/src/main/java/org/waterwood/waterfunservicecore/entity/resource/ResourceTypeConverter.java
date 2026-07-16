package org.waterwood.waterfunservicecore.entity.resource;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.waterwood.common.io.ResourceType;

@Converter(autoApply = true)
public class ResourceTypeConverter implements AttributeConverter<ResourceType, Byte> {

    @Override
    public Byte convertToDatabaseColumn(ResourceType attribute) {
        return attribute == null ? ResourceType.UNKNOWN.getValue() : attribute.getValue();
    }

    @Override
    public ResourceType convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : ResourceType.fromCode(dbData);
    }
}
