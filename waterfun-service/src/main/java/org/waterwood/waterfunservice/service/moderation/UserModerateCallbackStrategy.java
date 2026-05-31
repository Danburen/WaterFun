package org.waterwood.waterfunservice.service.moderation;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.waterwood.common.CloudFSRoot;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.api.message.ModerationConsumerMessage;
import org.waterwood.waterfunservicecore.entity.audit.AuditRejectType;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.resource.AuditResource;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.notification.InboxSystem;
import org.waterwood.waterfunservicecore.entity.notification.NoticeType;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;
import org.waterwood.waterfunservicecore.exception.reference.AuditTaskResourceReferenceInvalid;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelper;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.notification.InboxSystemRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudResOperationType;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;

import java.util.Locale;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserModerateCallbackStrategy implements ModerationCallbackStrategy {

    private final MessageSource messageSource;
    private final UserCoreService userCoreService;
    private final InboxSystemRepository inboxSystemRepository;
    private final AuditTaskRepository auditTaskRepository;
    private final AuditTaskResourceRepository auditTaskResourceRepository;
    private final UserRepository userRepository;
    private final CloudFileService cloudFileService;
    private final RedisHelper redisHelper;
    private final ResourceRepository resourceRepository;

    @Override
    public Set<TargetType> getTargetTypes() {
        return Set.of(TargetType.USER_AVATAR);
    }

    @Transactional
    @Override
    public void handle(ModerationConsumerMessage msg) {
        InboxSystem is = new InboxSystem();
        Long bizId = Long.parseLong(msg.getTargetId());
        Locale locale = Locale.of(msg.getUserLocale());
        switch (msg.getTargetType()){
            case USER_AVATAR: handleUserAvatarModeration(msg, is, bizId, locale); break;
        }
        is.setNoticeType(NoticeType.BUSINESS);
        is.setTitle(messageSource.getMessage("notification.audit.title", null, locale));
        is.setUser(userRepository.getReferenceById(bizId));
        inboxSystemRepository.save(is);
    }

    private void handleUserAvatarModeration(ModerationConsumerMessage msg, InboxSystem is, Long userUid, Locale locale) {
        AuditResource res = auditTaskResourceRepository.findByTaskId(msg.getId()).orElseThrow(
                () -> new AuditTaskResourceReferenceInvalid(msg.getId())
        );
        if(msg.getStatus() == AuditStatus.APPROVED){
            is.setContent(messageSource.getMessage("notification.audit.avatar.approve", null, locale));
            String dbAvatarResourceUuid = userCoreService.getUserAvatar(userUid);
            if(dbAvatarResourceUuid != null){
                resourceRepository.findByUuidAndStatus(dbAvatarResourceUuid, ResourceStatus.ACTIVE)
                        .ifPresentOrElse(
                                resource -> {
                                    cloudFileService.removeFile(CloudFSRoot.USER, resource.getResourceKey());
                                },
                                () -> {
                                    // Resource is manual deleted
                                    log.warn("User {}'s avatar resource {} is not found during moderation callback, it might be manually deleted", userUid, dbAvatarResourceUuid);
                                });
            }
            userCoreService.updateAvatarResourceUuid(userUid, res.getResource().getUuid());
            // Remove cached url in redis, so that new avatar can be fetched with new url
            String redisKey = cloudFileService.getCachedRedisKey(
                    userUid,
                    TargetType.USER_AVATAR,
                    CloudResOperationType.READ
            );
            redisHelper.del(redisKey);
        } else if(msg.getStatus() == AuditStatus.REJECTED){
            String fullMsg = getRejectText(msg.getRejectType(), locale) + " " + (StringUtil.isBlank(msg.getRejectReason()) ? "" : msg.getRejectReason());
            is.setContent(messageSource.getMessage("notification.audit.avatar.reject", new Object[]{fullMsg}, locale));
            cloudFileService.removeFile(CloudFSRoot.USER, res.getResource().getResourceKey());
        }
    }

    private String getRejectText(AuditRejectType type, Locale locale){
        String rejectTemplateText = switch (type) {
            case VIOLATION_OF_GUIDELINES -> "reject.reason.violation_of_guidelines";
            case INAPPROPRIATE_CONTENT -> "reject.reason.inappropriate_content";
            case ADVERTISEMENT -> "reject.reason.advertisement";
            case VIOLENCE -> "reject.reason.violence";
            case SENSITIVE -> "reject.reason.sensitive";
            case OTHER -> "reject.reason.other";
        };

        return messageSource.getMessage(rejectTemplateText, null, locale);
    }
}
