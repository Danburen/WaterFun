package org.waterwood.waterfunservice.service.moderation;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.api.message.ModerationConsumerMessage;
import org.waterwood.waterfunservicecore.entity.audit.AuditRejectType;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.notification.Inbox;
import org.waterwood.waterfunservicecore.entity.notification.NoticeType;
import org.waterwood.waterfunservicecore.infrastructure.persistence.notification.InboxRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ModerationInboxHandler {

    private final UserRepository userRepository;
    private final InboxRepository inboxRepository;
    private final MessageSource messageSource;

    @Transactional
    public void handleModeration(ModerationConsumerMessage msg, Long targetUserUid, Object... args) {
        Inbox is = buildInbox(msg, targetUserUid, args);
        inboxRepository.save(is);
    }

    private String getRejectText(AuditRejectType type, Locale locale){
        String rejectTemplateText = switch (type) {
            case VIOLATION_OF_GUIDELINES -> "reject.reason.violation_of_guidelines";
            case INAPPROPRIATE_CONTENT -> "reject.reason.inappropriate_content";
            case ADVERTISEMENT -> "reject.reason.advertisement";
            case VIOLENCE -> "reject.reason.violence";
            case SENSITIVE -> "reject.reason.sensitive";
            case CASCADE -> "reject.reason.cascade";
            case OTHER -> "reject.reason.other";
        };

        return messageSource.getMessage(rejectTemplateText, null, locale);
    }

    @Transactional
    public void handleBatch(List<ModerationConsumerMessage> msgs, Object... args) {
        if (msgs.isEmpty()) return;
        List<Inbox> inboxes = msgs.stream()
                .map(msg ->
                        buildInbox(msg, Long.parseLong(msg.getTargetId()), args)
                ).toList();
        inboxRepository.saveAll(inboxes);
    }
    private Inbox buildInbox(ModerationConsumerMessage msg, Long targetUserUid, Object[] args) {
        ModerationTargetType type = ModerationTargetType.fromTargetType(msg.getTargetType());
        Locale locale = Locale.of(msg.getUserLocale());

        Inbox is = new Inbox();
        is.setNoticeType(NoticeType.SYSTEM);
        is.setTitle(type.getTitle());
        is.setUser(userRepository.getReferenceById(targetUserUid));
        is.setTargetId(msg.getTargetId());
        is.setTargetType(msg.getTargetType());

        if (msg.getStatus() == AuditStatus.APPROVED) {
            is.setContent(messageSource.getMessage(type.getApprove(), args, locale));
        } else if (msg.getStatus() == AuditStatus.REJECTED) {
            String fullMsg = getRejectText(msg.getRejectType(), locale) + " " +
                    (StringUtil.isBlank(msg.getRejectReason()) ? "" : msg.getRejectReason());
            is.setContent(messageSource.getMessage(type.getReject(), new Object[]{args, fullMsg}, locale));
        }
        return is;
    }
}
