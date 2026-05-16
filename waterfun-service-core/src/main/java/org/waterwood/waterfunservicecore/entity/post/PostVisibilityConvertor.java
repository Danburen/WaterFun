package org.waterwood.waterfunservicecore.entity.post;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PostVisibilityConvertor implements AttributeConverter<PostVisibility, Short> {

    @Override
    public Short convertToDatabaseColumn(PostVisibility attribute) {
        return attribute == null ? 0 : attribute.getCode();
    }

    @Override
    public PostVisibility convertToEntityAttribute(Short dbData) {
        return PostVisibility.fromCode(dbData);
    }
}
