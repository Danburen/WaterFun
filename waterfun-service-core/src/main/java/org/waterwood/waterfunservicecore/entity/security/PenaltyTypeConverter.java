package org.waterwood.waterfunservicecore.entity.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PenaltyTypeConverter implements AttributeConverter<PenaltyType, Byte> {

    @Override
    public Byte convertToDatabaseColumn(PenaltyType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public PenaltyType convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : PenaltyType.fromValue(dbData);
    }
}
