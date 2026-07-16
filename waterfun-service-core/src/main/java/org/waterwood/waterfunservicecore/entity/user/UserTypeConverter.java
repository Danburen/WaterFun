package org.waterwood.waterfunservicecore.entity.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserTypeConverter implements AttributeConverter<UserType, Byte> {

    @Override
    public Byte convertToDatabaseColumn(UserType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public UserType convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : UserType.fromValue(dbData);
    }
}
