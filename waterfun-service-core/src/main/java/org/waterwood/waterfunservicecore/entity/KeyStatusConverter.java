package org.waterwood.waterfunservicecore.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class KeyStatusConverter implements AttributeConverter<KeyStatus, Byte> {

    @Override
    public Byte convertToDatabaseColumn(KeyStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public KeyStatus convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : KeyStatus.fromValue(dbData);
    }
}
