package org.waterwood.waterfunservice.service.moderation;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.api.message.ModerationConsumerMessage;
import org.waterwood.waterfunservicecore.entity.audit.AuditRejectType;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.notification.InboxSystem;
import org.waterwood.waterfunservicecore.entity.notification.NoticeType;
import org.waterwood.waterfunservicecore.infrastructure.persistence.notification.InboxSystemRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ModerationInboxHandler {

    private final UserRepository userRepository;
    private final InboxSystemRepository inboxSystemRepository;
    private final MessageSource messageSource;

    @Transactional
    public void handleModeration(ModerationConsumerMessage msg, Long targetUserUid, Object... args) {
        InboxSystem is = buildInbox(msg, targetUserUid, args);
        inboxSystemRepository.save(is);
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
        List<InboxSystem> inboxes = msgs.stream()
                .map(msg ->
                        buildInbox(msg, Long.parseLong(msg.getTargetId()), args)
                ).toList();
        inboxSystemRepository.saveAll(inboxes);
    }
    private InboxSystem buildInbox(ModerationConsumerMessage msg, Long targetUserUid, Object[] args) {
        ModerationTargetType type = ModerationTargetType.fromTargetType(msg.getTargetType());
        Locale locale = Locale.of(msg.getUserLocale());

        InboxSystem is = new InboxSystem();
        is.setNoticeType(NoticeType.BUSINESS);
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
