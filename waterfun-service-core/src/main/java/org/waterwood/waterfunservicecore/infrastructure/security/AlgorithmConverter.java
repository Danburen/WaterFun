package org.waterwood.waterfunservicecore.infrastructure.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AlgorithmConverter implements AttributeConverter<Algorithm, Byte> {

    @Override
    public Byte convertToDatabaseColumn(Algorithm attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public Algorithm convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : Algorithm.fromValue(dbData);
    }
}
