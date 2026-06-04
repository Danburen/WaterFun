package org.waterwood.waterfunservicecore.entity.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserTypeConverter implements AttributeConverter<UserType, Short> {

    @Override
    public Short convertToDatabaseColumn(UserType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public UserType convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : UserType.fromValue(dbData);
    }
}
