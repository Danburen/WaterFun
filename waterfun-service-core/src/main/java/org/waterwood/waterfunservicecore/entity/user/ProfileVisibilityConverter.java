package org.waterwood.waterfunservicecore.entity.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProfileVisibilityConverter implements AttributeConverter<ProfileVisibility, Short> {

    @Override
    public Short convertToDatabaseColumn(ProfileVisibility attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ProfileVisibility convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : ProfileVisibility.fromValue(dbData);
    }
}
