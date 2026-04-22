package org.waterwood.waterfunservicecore.entity.notification;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class NoticeTypeConvertor implements AttributeConverter<NoticeType, Short> {

    @Override
    public Short convertToDatabaseColumn(NoticeType noticeType) {
        return noticeType.getCode();
    }

    @Override
    public NoticeType convertToEntityAttribute(Short aShort) {
        return NoticeType.fromCode(aShort);
    }
}
