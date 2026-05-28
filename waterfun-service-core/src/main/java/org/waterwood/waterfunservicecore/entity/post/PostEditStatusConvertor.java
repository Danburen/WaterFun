package org.waterwood.waterfunservicecore.entity.post;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PostEditStatusConvertor implements AttributeConverter<PostEditStatus, Short> {

    @Override
    public Short convertToDatabaseColumn(PostEditStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public PostEditStatus convertToEntityAttribute(Short dbData) {
        return PostEditStatus.fromCode(dbData);
    }
}
