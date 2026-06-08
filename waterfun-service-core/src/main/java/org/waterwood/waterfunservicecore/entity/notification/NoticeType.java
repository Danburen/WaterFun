package org.waterwood.waterfunservicecore.entity.notification;

import lombok.Getter;

@Getter
public enum NoticeType {
    GENERAL(0, NoticeGroup.MISC, NoticePriority.MEDIUM),
    LIKE(1, NoticeGroup.INTERACTION, NoticePriority.MEDIUM),
    REPLY(2, NoticeGroup.REPLY, NoticePriority.HIGH),
    MENTION(3, NoticeGroup.MENTION, NoticePriority.EMERGENCY),
    NEW_FOLLOWER(4, NoticeGroup.INTERACTION, NoticePriority.MEDIUM),
    COLLECT(5, NoticeGroup.INTERACTION, NoticePriority.MEDIUM),
    PROMOTION(9, NoticeGroup.SYSTEM, NoticePriority.LOW),
    SYSTEM(10, NoticeGroup.SYSTEM, NoticePriority.MEDIUM),;


    private final NoticeGroup group;
    private final NoticePriority priority;
    private final short code;
    NoticeType(final int code, final NoticeGroup group, final NoticePriority priority) {
        this.code = (short) code;
        this.group = group;
        this.priority = priority;
    }

    public static NoticeType fromCode(final short code) {
        for (final NoticeType noticeType : NoticeType.values()) {
            if (noticeType.code == code) {
                return noticeType;
            }
        }
        throw new IllegalArgumentException("No NoticeType found for code " + code);
    }

    public boolean isSendImmediately() {
        return this.priority == NoticePriority.HIGH;
    }

    public boolean isAggregatable() {
        return this.group == NoticeGroup.INTERACTION || this.group == NoticeGroup.SYSTEM;
    }
}
