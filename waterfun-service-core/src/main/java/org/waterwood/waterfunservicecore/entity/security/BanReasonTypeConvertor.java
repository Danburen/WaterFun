package org.waterwood.waterfunservicecore.entity.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BanReasonTypeConvertor implements AttributeConverter<BanReasonType, Byte> {


    @Override
    public Byte convertToDatabaseColumn(BanReasonType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public BanReasonType convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : BanReasonType.fromValue(dbData);
    }
}
