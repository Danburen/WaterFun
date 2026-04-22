package org.waterwood.waterfunservicecore.entity.notification;

import lombok.Getter;

@Getter
public enum NoticeType {
    BUSINESS(1),
    ACCOUNT_SECURITY(2),
    PENALTY(3),;

    private final short code;
    NoticeType(final int code) {
        this.code = (short) code;
    }

    public static NoticeType fromCode(final short code) {
        for (final NoticeType noticeType : NoticeType.values()) {
            if (noticeType.code == code) {
                return noticeType;
            }
        }
        return BUSINESS;
    }
}
