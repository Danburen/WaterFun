package org.waterwood.waterfunservice.service.moderation;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.waterwood.common.CloudStorageRootKey;
import org.waterwood.common.KeyConstants;
import org.waterwood.utils.PathUtil;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.api.message.ModerationConsumerMessage;
import org.waterwood.waterfunservicecore.entity.audit.AuditRejectType;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.audit.AuditTaskResource;
import org.waterwood.waterfunservicecore.entity.audit.task.MediaResourceType;
import org.waterwood.waterfunservicecore.entity.notification.InboxSystem;
import org.waterwood.waterfunservicecore.entity.notification.NoticeType;
import org.waterwood.waterfunservicecore.exception.NotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelper;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.notification.InboxSystemRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudResOperationType;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;
import org.waterwood.waterfunservicecore.utils.BizPayload;
import org.waterwood.waterfunservicecore.utils.BizTargetIdPackager;

import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UserModerateStrategy implements ModerationStrategy{

    private final MessageSource messageSource;
    private final UserCoreService userCoreService;
    private final InboxSystemRepository inboxSystemRepository;
    private final AuditTaskRepository auditTaskRepository;
    private final AuditTaskResourceRepository auditTaskResourceRepository;
    private final UserRepository userRepository;
    private final CloudFileService cloudFileService;
    private final RedisHelper redisHelper;

    @Override
    public Set<MediaResourceType> getTargetTypes() {
        return Set.of(MediaResourceType.USER_AVATAR);
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
        AuditTaskResource res = auditTaskResourceRepository.findByTaskId(msg.getId()).orElseThrow(
                () -> new NotFoundException("Audit task resource not found for task id: " + msg.getId())
        );
        if(msg.getStatus() == AuditStatus.APPROVED){
            is.setContent(messageSource.getMessage("notification.audit.avatar.approve", null, locale));
            String originPath = res.getResourceKey();
            String targetPath = PathUtil.buildPath(PathUtil.buildPath(KeyConstants.AVATAR, res.getResourceKey()));
            String dbAvatarPath = userCoreService.getUserAvatar(userUid);
            // Copy from temp to uploads, and remove old avatar in uploads
            cloudFileService.copyFileAndRemoveOld(CloudStorageRootKey.TEMP, originPath, CloudStorageRootKey.UPLOADS, targetPath);
            // Remove db storage path if existed, which is the old avatar path
            cloudFileService.removeFile(CloudStorageRootKey.UPLOADS, dbAvatarPath);
            // Remove cached url in redis, so that new avatar can be fetched with new url
            String redisKey = cloudFileService.getCachedRedisKey(
                    userUid,
                    MediaResourceType.USER_AVATAR,
                    CloudResOperationType.READ
            );
            redisHelper.del(redisKey);

            userCoreService.updateAvatar(userUid, res.getResourceKey());

        } else if(msg.getStatus() == AuditStatus.REJECTED){
            String fullMsg = getRejectText(msg.getRejectType(), locale) + " " + (StringUtil.isBlank(msg.getRejectReason()) ? "" : msg.getRejectReason());
            is.setContent(messageSource.getMessage("notification.audit.avatar.reject", new Object[]{fullMsg}, locale));
            cloudFileService.removeFile(CloudStorageRootKey.TEMP, res.getResourceKey());
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
