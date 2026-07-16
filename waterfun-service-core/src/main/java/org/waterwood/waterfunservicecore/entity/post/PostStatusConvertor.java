package org.waterwood.waterfunservicecore.entity.post;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PostStatusConvertor implements AttributeConverter<PostStatus, Byte> {

    @Override
    public Byte convertToDatabaseColumn(PostStatus attribute) {
        return attribute == null ? 0 : attribute.getValue();
    }

    @Override
    public PostStatus convertToEntityAttribute(Byte dbData) {
        return PostStatus.fromCode(dbData);
    }
}
