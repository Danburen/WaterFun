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
        return switch (noticeType) {
            case REPLY -> {
                List<Long> uids = safeLongList(content.get("userUids"));
                Long replierUid = uids.isEmpty() ? null : uids.getFirst();
                Object rc = content.get("replyContent");
                yield new ReplyContentDTO(
                        replierUid,
                        rc instanceof String ? (String) rc : "",
                        safeString(content.get("nativeUrl")),
                        null
                );
            }
            case LIKE, COLLECT -> {
                yield new LikeContentDTO(
                        safeLongList(content.get("userUids")),
                        safeString(content.get("nativeUrl")),
                        null
                );
            }
            case NEW_FOLLOWER -> {
                Long followerUid = safeLong(content.get("followerUid"));
                String nativeText = safeString(content.get("nativeUrl"));
                yield new FollowerContentDTO(followerUid, nativeText);
            }
            default -> {
                String text = safeString(content.get("text"));
                yield new SystemContentDTO(text);
            }
        };
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