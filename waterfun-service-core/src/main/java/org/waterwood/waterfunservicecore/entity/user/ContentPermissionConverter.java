package org.waterwood.waterfunservicecore.entity.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ContentPermissionConverter implements AttributeConverter<ContentPermission, Byte> {

    @Override
    public Byte convertToDatabaseColumn(ContentPermission attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ContentPermission convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : ContentPermission.fromValue(dbData);
    }
}
