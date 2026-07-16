package org.waterwood.waterfunservicecore.entity.perm;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PermissionTypeConverter implements AttributeConverter<PermissionType, Byte> {

    @Override
    public Byte convertToDatabaseColumn(PermissionType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public PermissionType convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : PermissionType.fromValue(dbData);
    }
}
