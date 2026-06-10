package org.waterwood.waterfunservicecore.entity.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BanReasonTypeConvertor implements AttributeConverter<BanReasonType, Short> {


    @Override
    public Short convertToDatabaseColumn(BanReasonType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public BanReasonType convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : BanReasonType.fromValue(dbData);
    }
}
