package org.waterwood.waterfunservicecore.entity.audit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserActionTypeConverter implements AttributeConverter<UserActionType, Byte> {
    @Override
    public Byte convertToDatabaseColumn(UserActionType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public UserActionType convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : UserActionType.fromValue(dbData);
    }
}
