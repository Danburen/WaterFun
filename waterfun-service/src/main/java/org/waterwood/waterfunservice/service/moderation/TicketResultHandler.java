package org.waterwood.waterfunservice.service.moderation;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservice.api.response.InboxNotificationRes;
import org.waterwood.waterfunservice.infrastructure.mapper.InboxSystemMapper;
import org.waterwood.waterfunservice.service.SSEService;
import org.waterwood.waterfunservicecore.api.message.TicketMessage;
import org.waterwood.waterfunservicecore.entity.notification.Inbox;
import org.waterwood.waterfunservicecore.entity.notification.NoticeType;
import org.waterwood.waterfunservicecore.entity.ticket.TicketAuditStatus;
import org.waterwood.waterfunservicecore.entity.ticket.TicketType;
import org.waterwood.waterfunservicecore.infrastructure.persistence.notification.InboxRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ticket.TicketRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserPreferenceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserSettingRepository;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketResultHandler {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final InboxRepository inboxRepository;
    private final MessageSource messageSource;
    private final SSEService sseService;
    private final InboxSystemMapper inboxSystemMapper;
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserSettingRepository userSettingRepository;

    @Transactional
    public void handle(TicketMessage msg) {
        updateTicketStatus(msg);
        notifySubmitter(msg);
        // Notify the penalized user if applicable
        if (msg.getTargetUserUid() != null && msg.getPenaltyType() != null) {
            notifyTargetUser(msg);
        }
    }

    private void updateTicketStatus(TicketMessage msg) {
        try {
            ticketRepository.findById(msg.getTicketId()).ifPresent(ticket -> {
                ticket.setStatus(msg.getStatus());
                ticket.setUpdatedAt(Instant.now());
                ticketRepository.save(ticket);
            });
        } catch (Exception e) {
            log.warn("Failed to update Ticket status for ticket {}: {}", msg.getTicketId(), e.getMessage());
        }
    }

    private boolean isEventNotificationAllowed(Long userUid) {
        return userSettingRepository.findById(userUid)
                .map(s -> Boolean.TRUE.equals(s.getEventNotifications()))
                .orElse(true);
    }

    private void notifySubmitter(TicketMessage msg) {
        if (!isEventNotificationAllowed(msg.getSubmitterId())) return;
        try {
            Inbox inbox = buildInbox(msg);
            inboxRepository.save(inbox);
            pushInboxToUser(msg.getSubmitterId(), inbox);
        } catch (Exception e) {
            log.warn("Failed to create ticket notification for submitter {}: {}", msg.getTicketId(), e.getMessage());
        }
    }

    private void notifyTargetUser(TicketMessage msg) {
        if (!isEventNotificationAllowed(msg.getTargetUserUid())) return;
        try {
            String storedLocale = userPreferenceRepository.getLocaleByUserUid(msg.getTargetUserUid());
            Locale locale = StringUtil.isBlank(storedLocale) ? Locale.getDefault() : Locale.forLanguageTag(storedLocale);

            String penaltyName = messageSource.getMessage(
                    "ticket.penalty." + msg.getPenaltyType().name().toLowerCase(),
                    null,
                    msg.getPenaltyType().name(),
                    locale
            );

            Inbox inbox = new Inbox();
            inbox.setNoticeType(NoticeType.SYSTEM);
            inbox.setTitle(messageSource.getMessage("notification.audit.penalty.title", null, locale));
            inbox.setUser(userRepository.getReferenceById(msg.getTargetUserUid()));
            inbox.setTargetId(msg.getTargetId());
            inbox.setContent(Map.of("text",
                    messageSource.getMessage("notification.audit.penalty.content",
                            new Object[]{msg.getTargetId(), penaltyName}, locale)));

            inboxRepository.save(inbox);
            pushInboxToUser(msg.getTargetUserUid(), inbox);
        } catch (Exception e) {
            log.warn("Failed to create penalty notification for target user {}: {}", msg.getTargetUserUid(), e.getMessage());
        }
    }

    private Inbox buildInbox(TicketMessage msg) {
        String storedLocale = userPreferenceRepository.getLocaleByUserUid(msg.getSubmitterId());
        Locale locale = StringUtil.isBlank(storedLocale) ? Locale.getDefault() : Locale.forLanguageTag(storedLocale);

        Inbox inbox = new Inbox();
        inbox.setNoticeType(NoticeType.SYSTEM);
        inbox.setTitle(messageSource.getMessage("notification.audit.ticket.title", null, locale));
        inbox.setUser(userRepository.getReferenceById(msg.getSubmitterId()));
        inbox.setTargetId(msg.getTargetId());

        String text = resolveMessageContent(msg, locale);
        inbox.setContent(Map.of("text", text));

        return inbox;
    }

    /**
     * Resolve the notification text for a ticket result message.
     * Priority: custom replyContent > default template based on ticketType + status.
     */
    private String resolveMessageContent(TicketMessage msg, Locale locale) {
        // 1. Custom reply from admin (最高优先级)
        if (StringUtil.isNotBlank(msg.getReplyContent())) {
            return msg.getReplyContent();
        }

        // 2. Default template by ticket type + status
        TicketType ticketType = msg.getTicketType();
        TicketAuditStatus status = msg.getStatus();
        String msgKey = resolveDefaultMessageKey(msg, ticketType, status);
        if (msgKey == null) {
            // Fallback to generic template
            String fallbackKey = status == TicketAuditStatus.RESOLVED
                    ? "notification.audit.ticket.approve"
                    : "notification.audit.ticket.reject_args";
            return messageSource.getMessage(fallbackKey, null, locale);
        }

        Object[] args = resolveMessageArgs(msg, ticketType, status, locale);
        return messageSource.getMessage(msgKey, args, locale);
    }

    private String resolveDefaultMessageKey(TicketMessage msg, TicketType ticketType,
                                             TicketAuditStatus status) {
        return switch (ticketType) {
            case CONTENT_REPORT -> switch (status) {
                case RESOLVED -> msg.getPenaltyType() != null
                        ? "ticket.message.content_report.approve_with_penalty"
                        : "ticket.message.content_report.approve";
                case REJECTED -> "ticket.message.content_report.reject";
                default -> null;
            };
            case ACCOUNT_APPEAL -> switch (status) {
                case RESOLVED -> "ticket.message.account_appeal.approve";
                case REJECTED -> "ticket.message.account_appeal.reject";
                default -> null;
            };
            case SUGGESTION -> switch (status) {
                case RESOLVED -> "ticket.message.suggestion.approve";
                default -> null;
            };
            case FEATURE_FEEDBACK -> switch (status) {
                case RESOLVED -> "ticket.message.feedback.approve";
                case REJECTED -> "ticket.message.feedback.reject";
                default -> null;
            };
        };
    }

    private Object[] resolveMessageArgs(TicketMessage msg, TicketType ticketType,
                                         TicketAuditStatus status, Locale locale) {
        if (ticketType == TicketType.CONTENT_REPORT && status == TicketAuditStatus.RESOLVED
                && msg.getPenaltyType() != null) {
            String targetInfo = msg.getTargetType().toLowerCase() + " #" + msg.getTargetId();
            String penaltyName = messageSource.getMessage(
                    "ticket.penalty." + msg.getPenaltyType().name().toLowerCase(),
                    null,
                    msg.getPenaltyType().name(),
                    locale
            );
            return new Object[]{targetInfo, penaltyName};
        }
        if (status == TicketAuditStatus.REJECTED) {
            String rejectText = msg.getRejectType() != null
                    ? messageSource.getMessage(msg.getRejectType().getMessageKey(), null, locale)
                    : "";
            String fullMsg = rejectText + " " +
                    (StringUtil.isBlank(msg.getRejectReason()) ? "" : msg.getRejectReason());
            return new Object[]{fullMsg.trim()};
        }
        return null;
    }

    private void pushInboxToUser(Long recipient, Inbox inbox) {
        try {
            InboxNotificationRes dto = inboxSystemMapper.toDto(inbox);
            boolean pushed = sseService.sendToUser(recipient, dto);
            if (pushed) {
                log.debug("SSE pushed ticket notification to user {}", recipient);
            }
        } catch (Exception e) {
            log.warn("Failed to push SSE ticket notification to user {}: {}", recipient, e.getMessage());
        }
    }
}
