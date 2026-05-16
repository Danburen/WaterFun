package org.waterwood.waterfunservicecore.entity.post;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PostStatusConvertor implements AttributeConverter<PostStatus, Short> {

    @Override
    public Short convertToDatabaseColumn(PostStatus attribute) {
        return attribute == null ? 0 : attribute.getValue();
    }

    @Override
    public PostStatus convertToEntityAttribute(Short dbData) {
        return PostStatus.fromCode(dbData);
    }
}
