package org.waterwood.waterfunservicecore.entity.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PenaltyTypeConverter implements AttributeConverter<PenaltyType, Short> {

    @Override
    public Short convertToDatabaseColumn(PenaltyType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public PenaltyType convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : PenaltyType.fromValue(dbData);
    }
}
