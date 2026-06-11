package org.waterwood.waterfunservicecore.entity.notification;

import lombok.Getter;
import org.waterwood.waterfunservicecore.entity.Priority;

@Getter
public enum NoticeType {
    GENERAL(0, NoticeGroup.MISC, Priority.MEDIUM),
    LIKE(1, NoticeGroup.INTERACTION, Priority.MEDIUM),
    REPLY(2, NoticeGroup.REPLY, Priority.HIGH),
    MENTION(3, NoticeGroup.MENTION, Priority.EMERGENCY),
    NEW_FOLLOWER(4, NoticeGroup.INTERACTION, Priority.MEDIUM),
    COLLECT(5, NoticeGroup.INTERACTION, Priority.MEDIUM),
    PROMOTION(9, NoticeGroup.SYSTEM, Priority.LOW),
    SYSTEM(10, NoticeGroup.SYSTEM, Priority.MEDIUM),;


    private final NoticeGroup group;
    private final Priority priority;
    private final short code;
    NoticeType(final int code, final NoticeGroup group, final Priority priority) {
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
        return this.priority == Priority.HIGH;
    }

    public boolean isAggregatable() {
        return this.group == NoticeGroup.INTERACTION || this.group == NoticeGroup.SYSTEM;
    }
}
