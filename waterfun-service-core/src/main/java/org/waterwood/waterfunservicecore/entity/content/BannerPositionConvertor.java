package org.waterwood.waterfunservicecore.entity.content;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BannerPositionConvertor implements AttributeConverter<BannerPosition, Byte> {

    @Override
    public Byte convertToDatabaseColumn(BannerPosition attribute) {
        return attribute == null ? 1 : attribute.getCode();
    }

    @Override
    public BannerPosition convertToEntityAttribute(Byte dbData) {
        return BannerPosition.fromCode(dbData);
    }
}
