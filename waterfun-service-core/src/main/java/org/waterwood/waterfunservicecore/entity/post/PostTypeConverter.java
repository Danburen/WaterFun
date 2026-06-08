package org.waterwood.waterfunservicecore.entity.post;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PostTypeConverter implements AttributeConverter<PostType, Short> {

    @Override
    public Short convertToDatabaseColumn(PostType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public PostType convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : PostType.fromValue(dbData);
    }
}
