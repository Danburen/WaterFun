package org.waterwood.waterfunservicecore.entity.notification;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BusinessTypeConverter implements AttributeConverter<BusinessType, Short> {

    @Override
    public Short convertToDatabaseColumn(BusinessType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public BusinessType convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : BusinessType.fromValue(dbData);
    }
}
