package org.waterwood.waterfunservice.service.moderation;

import lombok.Getter;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;

import java.util.Set;

@Getter
public enum ModerationTargetType {
    USER_AVATAR( "notification.audit.title",
            "notification.audit.avatar.approve",
            "notification.audit.avatar.reject_args",
            Set.of(TargetType.USER_AVATAR)
    ),
    POST("notification.audit.title",
            "notification.audit.post.approve",
             "notification.audit.post.reject_args",
             Set.of(TargetType.POST, TargetType.POST_CONTENT_IMAGE, TargetType.POST_COVERAGE_IMAGE)
    ),
    TICKET("notification.audit.ticket.title",
            "notification.audit.ticket.approve",
            "notification.audit.ticket.reject_args",
            Set.of(TargetType.DEFAULT)
    ),;
    private final String title;
    private final String approve;
    private final String reject;
    private final Set<TargetType> typeSet;
    ModerationTargetType(String title, String approve, String reject, Set<TargetType> typeSet) {
        this.title = title;
        this.approve = approve;
        this.reject = reject;
        this.typeSet = typeSet;
    }

    public static ModerationTargetType fromTargetType(TargetType targetType) {
        for (ModerationTargetType type : ModerationTargetType.values()) {
            if (type.typeSet.contains(targetType)) {
                return type;
            }
        }
        throw new IllegalArgumentException("TargetType not supported");
    }
}
