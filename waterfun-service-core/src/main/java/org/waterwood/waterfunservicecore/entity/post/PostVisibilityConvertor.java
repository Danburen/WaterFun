package org.waterwood.waterfunservicecore.entity.post;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PostVisibilityConvertor implements AttributeConverter<PostVisibility, Byte> {

    @Override
    public Byte convertToDatabaseColumn(PostVisibility attribute) {
        return attribute == null ? 0 : attribute.getCode();
    }

    @Override
    public PostVisibility convertToEntityAttribute(Byte dbData) {
        return PostVisibility.fromCode(dbData);
    }
}
