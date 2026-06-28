package org.waterwood.waterfunservicecore.entity.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ContentPermissionConverter implements AttributeConverter<ContentPermission, Short> {

    @Override
    public Short convertToDatabaseColumn(ContentPermission attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ContentPermission convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : ContentPermission.fromValue(dbData);
    }
}
