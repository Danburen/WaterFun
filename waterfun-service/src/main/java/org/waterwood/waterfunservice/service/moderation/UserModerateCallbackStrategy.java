package org.waterwood.waterfunservice.service.moderation;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.waterwood.utils.CollectionUtil;
import org.waterwood.waterfunservicecore.api.message.ModerationConsumerMessage;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.resource.AuditResource;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelper;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.AuditTaskResourceRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.notification.InboxRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudResOperationType;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserModerateCallbackStrategy implements ModerationCallbackStrategy {

    private final MessageSource messageSource;
    private final UserCoreService userCoreService;
    private final InboxRepository inboxRepository;
    private final AuditTaskRepository auditTaskRepository;
    private final AuditTaskResourceRepository auditTaskResourceRepository;
    private final UserRepository userRepository;
    private final CloudFileService cloudFileService;
    private final RedisHelper redisHelper;
    private final ResourceRepository resourceRepository;
    private final ModerationInboxHandler moderationInboxMessageHandler;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Set<TargetType> getTargetTypes() {
        return Set.of(TargetType.USER_AVATAR);
    }

    @Transactional
    @Override
    public void handle(ModerationConsumerMessage msg) {
        Long bizId = Long.parseLong(msg.getTargetId());
        switch (msg.getTargetType()){
            case USER_AVATAR -> handleUserAvatarModeration(msg, bizId);
            default -> log.warn("Unhandled target type: {}", msg.getTargetType());
        }
        moderationInboxMessageHandler.handleModeration(msg, bizId);
    }

    @Override
    public void handleBatch(List<ModerationConsumerMessage> msgs){
        if (msgs.isEmpty()) return;
        Map<TargetType, List<ModerationConsumerMessage>> byType = msgs.stream()
                .collect(Collectors.groupingBy(ModerationConsumerMessage::getTargetType));
        byType.forEach((type, typeMsgs) -> {
            switch (type) {
                case USER_AVATAR -> handleUserAvatarBatch(typeMsgs);
                default -> log.warn("Unhandled target type in batch: {}", type);
            }
        });
    }
    @Transactional
    public void handleUserAvatarBatch(List<ModerationConsumerMessage> msgs) {
        Map<AuditStatus, List<ModerationConsumerMessage>> byStatus = msgs.stream()
                .collect(Collectors.groupingBy(ModerationConsumerMessage::getStatus));

        if (byStatus.isEmpty()) return;

        List<Long> allTaskIds = msgs.stream()
                .map(ModerationConsumerMessage::getId)
                .distinct()
                .toList();

        Map<Long, AuditResource> taskIdToResource = auditTaskResourceRepository.findByTaskIdIn(allTaskIds)
                .stream()
                .collect(Collectors.toMap(
                        r -> r.getId().getTaskId(),
                        Function.identity(),
                        (a, b) -> a
                ));

        byStatus.forEach((status, messages) -> {
            Map<Long, AuditResource> uidToResource = messages.stream()
                    .filter(m -> taskIdToResource.containsKey(m.getId()))
                    .collect(Collectors.toMap(
                            m -> Long.parseLong(m.getTargetId()),
                            m -> taskIdToResource.get(m.getId()),
                            (a, b) -> a
                    ));

            if (status == AuditStatus.APPROVED) {
                handleApproved(uidToResource);
            } else if (status == AuditStatus.REJECTED) {
                List<String> uuids = uidToResource.values().stream()
                        .map(r -> r.getId().getResourceUuid())
                        .toList();
                resourceRepository.batchUpdateStatus(ResourceStatus.ORPHAN, uuids);
            }
        });
        moderationInboxMessageHandler.handleBatch(msgs);
    }

    private void handleApproved(Map<Long, AuditResource> uidToResource) {
        List<Long> userUids = new ArrayList<>(uidToResource.keySet());
        Map<Long, String> uidToUuid = uidToResource.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().getId().getResourceUuid()
                ));

        List<Resource> oldAvatars = userRepository.findUserAvatarByUidIn(userUids);
        oldAvatars.forEach(r -> r.setStatus(ResourceStatus.ORPHAN));
        resourceRepository.saveAll(oldAvatars);

        List<String> newUuids = uidToResource.values().stream()
                .map(r -> r.getId().getResourceUuid())
                .distinct()
                .toList();
        Map<String, Resource> uuidToResource = resourceRepository.findByUuidIn(newUuids)
                .stream()
                .collect(Collectors.toMap(Resource::getUuid, Function.identity()));

        List<User> users = userRepository.findAllByUidIn(userUids);
        users.forEach(u -> {
            String uuid = uidToResource.get(u.getUid()).getId().getResourceUuid();
            u.setAvatarResource(uuidToResource.get(uuid));
        });
        userRepository.saveAll(users);

        List<String> redisKeys = cloudFileService.batchGetCachedRedisKey(
                CollectionUtil.serialize(userUids), TargetType.USER_AVATAR, CloudResOperationType.READ
        );
        redisHelper.del(redisKeys);
    }

    private void handleUserAvatarModeration(ModerationConsumerMessage msg, Long userUid) {
        AuditResource auditRes = auditTaskResourceRepository.findByTaskId(msg.getId()).orElseThrow(
                () -> new IllegalArgumentException("AuditResource not found for task id: " + msg.getId())
        );
        if(msg.getStatus() == AuditStatus.APPROVED){
            String dbAvatarResourceUuid = userCoreService.getUserAvatar(userUid);
            if(dbAvatarResourceUuid != null){
                resourceRepository.findByUuidAndStatus(dbAvatarResourceUuid, ResourceStatus.ACTIVE)
                        .ifPresentOrElse(
                                resource -> {
                                   resource.setStatus(ResourceStatus.ORPHAN);
                                   resourceRepository.save(resource);
                                },
                                () -> {
                                    // Resource is manual deleted
                                    log.warn("User {}'s avatar resource {} is not found during moderation callback, it might be manually deleted", userUid, dbAvatarResourceUuid);
                                });
            }
            userCoreService.updateAvatarResourceUuid(userUid, auditRes.getResource().getUuid());
            // Remove cached url in redis, so that new avatar can be fetched with new url
            String redisKey = cloudFileService.getCachedRedisKey(
                    userUid,
                    TargetType.USER_AVATAR,
                    CloudResOperationType.READ
            );
            redisHelper.del(redisKey);
        } else if(msg.getStatus() == AuditStatus.REJECTED){
            Resource res = auditRes.getResource();
            res.setStatus(ResourceStatus.ORPHAN);
            resourceRepository.save(res);
        }
    }
}
