package org.waterwood.waterfunservicecore.entity.content;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BannerPositionConvertor implements AttributeConverter<BannerPosition, Short> {

    @Override
    public Short convertToDatabaseColumn(BannerPosition attribute) {
        return attribute == null ? 1 : attribute.getCode();
    }

    @Override
    public BannerPosition convertToEntityAttribute(Short dbData) {
        return BannerPosition.fromCode(dbData);
    }
}
