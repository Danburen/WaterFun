package org.waterwood.waterfunservice.service.moderation;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservice.api.response.InboxNotificationRes;
import org.waterwood.waterfunservice.infrastructure.mapper.InboxSystemMapper;
import org.waterwood.waterfunservice.service.SSEService;
import org.waterwood.waterfunservicecore.api.message.ModerationConsumerMessage;
import org.waterwood.waterfunservicecore.entity.audit.AuditRejectType;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.notification.Inbox;
import org.waterwood.waterfunservicecore.entity.notification.NoticeType;
import org.waterwood.waterfunservicecore.infrastructure.persistence.notification.InboxRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j

@Component
@RequiredArgsConstructor
public class ModerationInboxHandler {

    private final UserRepository userRepository;
    private final InboxRepository inboxRepository;
    private final MessageSource messageSource;
    private final SSEService sseService;
    private final InboxSystemMapper inboxSystemMapper;

    @Transactional
    public void handleModeration(ModerationConsumerMessage msg, Long targetUserUid, Object... args) {
        Inbox is = buildInbox(msg, targetUserUid, args);
        inboxRepository.save(is);
        pushInboxToUser(targetUserUid, is);
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
        inboxes.forEach(inbox ->
                pushInboxToUser(inbox.getUser().getUid(), inbox)
        );
    }
    private void pushInboxToUser(Long recipient, Inbox inbox) {
        try {
            InboxNotificationRes dto = inboxSystemMapper.toDto(inbox);
            boolean pushed = sseService.sendToUser(recipient, dto);
            if (pushed) {
                log.debug("SSE pushed moderation notification to user {}", recipient);
            }
        } catch (Exception e) {
            log.warn("Failed to push SSE moderation notification to user {}: {}", recipient, e.getMessage());
        }
    }

    private Inbox buildInbox(ModerationConsumerMessage msg, Long targetUserUid, Object[] args) {
        ModerationTargetType type = ModerationTargetType.fromTargetType(msg.getTargetType());
        Locale locale = Locale.of(msg.getUserLocale());

        Inbox is = new Inbox();
        is.setNoticeType(NoticeType.SYSTEM);
        is.setTitle(type.getTitle());
        is.setUser(userRepository.getReferenceById(targetUserUid));
        is.setTargetId(msg.getTargetId());

        if (msg.getStatus() == AuditStatus.APPROVED) {
            is.setContent(Map.of("text", messageSource.getMessage(type.getApprove(), args, locale)));
        } else if (msg.getStatus() == AuditStatus.REJECTED) {
            String fullMsg = getRejectText(msg.getRejectType(), locale) + " " +
                    (StringUtil.isBlank(msg.getRejectReason()) ? "" : msg.getRejectReason());
            is.setContent(Map.of("text", messageSource.getMessage(type.getReject(), new Object[]{args, fullMsg}, locale)));
        }
        return is;
    }
}
