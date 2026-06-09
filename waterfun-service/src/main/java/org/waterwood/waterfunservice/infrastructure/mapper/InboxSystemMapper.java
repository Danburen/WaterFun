package org.waterwood.waterfunservice.infrastructure.mapper;

import org.mapstruct.*;
import org.waterwood.waterfunservice.api.response.*;
import org.waterwood.waterfunservicecore.entity.notification.Inbox;
import org.waterwood.waterfunservicecore.entity.notification.NoticeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface InboxSystemMapper {

    @Mapping(target = "noticeType", ignore = true)
    @Mapping(target = "content", ignore = true)
    Inbox toEntity(InboxNotificationRes inboxNotificationRes);

    @Mapping(target = "noticeType", expression = "java(inbox.getNoticeType().getCode())")
    @Mapping(target = "content", expression = "java(mapContent(inbox.getNoticeType(), inbox.getContent()))")
    InboxNotificationRes toDto(Inbox inbox);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "noticeType", ignore = true)
    @Mapping(target = "content", ignore = true)
    Inbox partialUpdate(InboxNotificationRes inboxNotificationRes, @MappingTarget Inbox inbox);

    default NotificationContent mapContent(NoticeType noticeType, Map<String, Object> content) {
        if (content == null) return null;
        switch (noticeType) {
            case REPLY: {
                List<Long> uids = safeLongList(content.get("userUids"));
                Long replierUid = uids.isEmpty() ? null : uids.getFirst();
                Object rc = content.get("replyContent");
                return new ReplyContentDTO(
                        replierUid,
                        rc instanceof String ? (String) rc : "",
                        safeString(content.get("nativeUrl"))
                );
            }
            case LIKE:
            case COLLECT:
                return new LikeContentDTO(
                        safeLongList(content.get("userUids")),
                        safeLong(content.get("imageUuid")),
                        safeString(content.get("nativeUrl"))
                );
            case NEW_FOLLOWER:
                return new FollowerContentDTO(
                        safeLong(content.get("followerUid")),
                        safeString(content.get("nativeUrl"))
                );
            default:
                return new SystemContentDTO(
                        safeString(content.get("text"))
                );
        }
    }

    default Long safeLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException e) { return null; }
    }

    default String safeString(Object value) {
        return value == null ? "" : value.toString();
    }

    @SuppressWarnings("unchecked")
    default List<Long> safeLongList(Object value) {
        if (value == null) return List.of();
        if (value instanceof List<?> rawList) {
            List<Long> result = new ArrayList<>();
            for (Object item : rawList) {
                if (item instanceof Number n) result.add(n.longValue());
                else if (item instanceof String s) {
                    try { result.add(Long.parseLong(s)); } catch (NumberFormatException ignored) {}
                }
            }
            return result;
        }
        return List.of();
    }
}