package org.waterwood.waterfunservicecore.entity.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AccountStatusConverter implements AttributeConverter<AccountStatus, Byte> {

    @Override
    public Byte convertToDatabaseColumn(AccountStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public AccountStatus convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : AccountStatus.fromValue(dbData);
    }
}
