package org.waterwood.waterfunservicecore.entity.notification;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class NoticeTypeConvertor implements AttributeConverter<NoticeType, Byte> {

    @Override
    public Byte convertToDatabaseColumn(NoticeType noticeType) {
        return noticeType.getCode();
    }

    @Override
    public NoticeType convertToEntityAttribute(Byte aShort) {
        return NoticeType.fromCode(aShort);
    }
}
