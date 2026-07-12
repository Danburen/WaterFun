package org.waterwood.waterfunservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.waterfunservice.api.response.InboxNotificationRes;
import org.waterwood.waterfunservice.infrastructure.dto.InboxPayload;
import org.waterwood.waterfunservice.infrastructure.dto.ReactionInboxPayload;
import org.waterwood.waterfunservice.infrastructure.dto.MultiUserIncludedInboxPayload;
import org.waterwood.waterfunservice.infrastructure.dto.ReplyInboxPayload;
import org.waterwood.waterfunservicecore.entity.notification.BusinessType;
import org.waterwood.waterfunservice.infrastructure.dto.FollowerInboxPayload;
import org.waterwood.waterfunservicecore.entity.notification.Inbox;
import org.waterwood.waterfunservicecore.entity.notification.NoticeType;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;
import org.waterwood.waterfunservicecore.exception.ForbiddenException;
import org.waterwood.utils.CollectionUtil;
import org.waterwood.waterfunservice.api.request.notifications.BatchMarkReadReq;
import org.waterwood.waterfunservice.infrastructure.mapper.InboxSystemMapper;
import org.waterwood.waterfunservicecore.api.CursorPage;
import org.waterwood.waterfunservicecore.exception.notfound.NotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PostRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.ResourceRepository;

import org.waterwood.waterfunservicecore.infrastructure.persistence.notification.InboxRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserSettingRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.waterwood.common.CloudFSRoot;
import org.waterwood.waterfunservice.api.response.NotificationCoverageContent;
import org.waterwood.waterfunservice.api.response.notifications.UnreadCountResp;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;

@Slf4j

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final InboxRepository inboxRepository;
    private final InboxSystemMapper inboxSystemMapper;
    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final SSEService sseService;
    private final CloudFileService cloudFileService;
    private final PostRepository postRepository;
    private final ResourceRepository resourceRepository;

    @Override
    public CursorPage<InboxNotificationRes, Long> list(Long cursor, Integer limit, Boolean unreadOnly, List<NoticeType> types) {
        limit = Math.min(limit, 20);
        List<Inbox> list = inboxRepository.findByCursor(
                UserCtxHolder.getUserUid(),
                cursor,
                unreadOnly,
                types == null || types.isEmpty() ? null : types,
                limit + 1
        );

        boolean hasNext = list.size() > limit;
        if(hasNext) {
            list = list.subList(0, limit);
        }
        Long nextCursor =  hasNext ? list.getLast().getId() : null;
        List<InboxNotificationRes> dtos = list.stream()
                .map(inboxSystemMapper::toDto)
                .toList();
        batchResolveCoverage(list, dtos);
        return  new CursorPage<>(
                dtos,
                nextCursor,
                hasNext
        );
    }

    @Override
    public Integer countAllUnread() {
        return inboxRepository.countByIsReadAndUserUidAndIsDeleted(Boolean.FALSE, UserCtxHolder.getUserUid(), false);
    }

    @Override
    public UnreadCountResp getUnreadCountWithTabs() {
        Long userId = UserCtxHolder.getUserUid();
        int total = inboxRepository.countByIsReadAndUserUidAndIsDeleted(Boolean.FALSE, userId, false);
        List<Object[]> rows = inboxRepository.countUnreadGroupedByNoticeType(userId);
        long system = 0, subscribe = 0, reply = 0, mention = 0;
        for (Object[] row : rows) {
            NoticeType type = (NoticeType) row[0];
            long count = ((Number) row[1]).longValue();
            switch (type) {
                case PROMOTION, SYSTEM -> system += count;
                case LIKE, NEW_FOLLOWER, COLLECT -> subscribe += count;
                case REPLY -> reply += count;
                case MENTION -> mention += count;
            }
        }
        Map<String, Long> tabs = new HashMap<>();
        tabs.put("system", system);
        tabs.put("subscribe", subscribe);
        tabs.put("reply", reply);
        tabs.put("mention", mention);
        return new UnreadCountResp(total, tabs);
    }

    @Transactional
    @Override
    public void markInboxRead(Long id) {
        Inbox is = inboxRepository.findById(id).orElseThrow(
                () -> NotFoundException.of("InboxSystem id not found: " + id)
        );

        if(! is.getUser().getUid().equals(UserCtxHolder.getUserUid())) {
            throw new ForbiddenException();
        }
        is.setIsRead(Boolean.TRUE);
        inboxRepository.save(is);
    }

    @Transactional
    @Override
    public void markSystemNotificationAllRead() {
        inboxRepository.markAllReadByUserUid(UserCtxHolder.getUserUid(), Instant.now());
    }

    @Transactional
    @Override
    public BatchResult batchMarkSystemNotificationRead(BatchMarkReadReq req) {
        int marked = 0;
        if(CollectionUtil.isNotEmpty(req.getIds())){
            marked = inboxRepository.markReadBatch(req.getIds(), UserCtxHolder.getUserUid(), Instant.now());
        }
        return BatchResult.ofNullable(req.getIds(), marked);
    }

    @Override
    public void handleAggregateNewInbox(Long recipient, Long userUid, NoticeType type, BusinessType businessType,
                                        Serializable targetBzId, String title, MultiUserIncludedInboxPayload payload) {
        if (recipient == null || recipient.equals(userUid)) return;
        String targetId = targetBzId.toString();
        List<Inbox> inboxes = inboxRepository
                .findByUserUidAndNoticeTypeAndBusinessTypeAndTargetIdOrderByCreatedAtDesc(
                        recipient,
                        type,
                        businessType,
                        targetId
                );
        if(inboxes.isEmpty()) {
            handleSingleNewInbox(recipient, type, businessType, targetId, title, payload);
        } else {
            Inbox latest = inboxes.getFirst();

            if (inboxes.size() > 1) {
                for (int i = 1; i < inboxes.size(); i++) {
                    inboxes.get(i).setIsDeleted(Boolean.TRUE);
                }
                inboxRepository.saveAll(inboxes.subList(1, inboxes.size()));
            }

            LinkedHashSet<Long> uniqueUserIds = new LinkedHashSet<>();
            for (Inbox inbox : inboxes) {
                MultiUserIncludedInboxPayload exists = payload.formMap(inbox.getContent());
                for (Long uid : exists.getUserUids()) {
                    uniqueUserIds.add(uid);
                    if (uniqueUserIds.size() >= 3) break;
                }
                if (uniqueUserIds.size() >= 3) break;
            }

            latest.setContent(payload.withUserUids(uniqueUserIds).toMap());
            latest.setAggregateCount((long) (inboxes.size() + 1));
            latest.setIsAggregated(Boolean.TRUE);
            latest.setIsDeleted(Boolean.FALSE);
            latest.setIsRead(Boolean.FALSE);
            latest.setCreatedAt(Instant.now());

            inboxRepository.save(latest);
            pushInboxToUser(recipient, latest);
        }
    }

    private boolean isNotificationAllowed(Long recipient, String settingField) {
        return userSettingRepository.findById(recipient)
                .map(s -> {
                    boolean allowed = switch (settingField) {
                        case "comment" -> s.getCommentNotifications();
                        case "like" -> s.getLikeNotifications();
                        case "follow" -> s.getFollowNotifications();
                        case "message" -> s.getMessageNotifications();
                        case "event" -> s.getEventNotifications();
                        default -> true;
                    };
                    return Boolean.TRUE.equals(allowed);
                })
                .orElse(true);
    }

    @Override
    public void onCommentLike(Long recipient, Long userUid, Long commentId, String title, Long postId) {
        if (!isNotificationAllowed(recipient, "like")) return;
        if(recipient.equals(userUid)) {
            return;
        }
        ReactionInboxPayload payload = new ReactionInboxPayload(
                List.of(userUid),
                null,
                "/post/" + postId + "#comment-" + commentId
        );
        handleAggregateNewInbox(recipient, userUid, NoticeType.LIKE, BusinessType.COMMENT, commentId, title, payload);
    }

    @Override
    public void onPostLike(Long recipient, Long userUid, Long postId, String title, String coverageResourceUuid) {
        if (recipient == null || recipient.equals(userUid)) return;
        if (!isNotificationAllowed(recipient, "like")) return;
        ReactionInboxPayload payload = new ReactionInboxPayload(
                List.of(userUid),
                coverageResourceUuid,
                "/post/" + postId
        );
        handleAggregateNewInbox(recipient, userUid, NoticeType.LIKE, BusinessType.POST, postId, title, payload);
    }

    @Override
    public void onPostCollect(Long recipient, Long userUid, Long postId, String title, String coverageResourceUuid) {
        if (recipient == null || recipient.equals(userUid)) return;
        if (!isNotificationAllowed(recipient, "like")) return;
        ReactionInboxPayload payload = new ReactionInboxPayload(
                List.of(userUid),
                coverageResourceUuid,
                "/post/" + postId
        );
        handleAggregateNewInbox(recipient, userUid, NoticeType.COLLECT, BusinessType.POST, postId, title, payload);
    }

    @Override
    public void onReply(Long recipient, Long userUid, Long commentId, String title, Long postId, String replyContent) {
        if (recipient == null || recipient.equals(userUid)) return; // no self notification
        if (!isNotificationAllowed(recipient, "comment")) return;
        ReplyInboxPayload payload = new ReplyInboxPayload(
                List.of(userUid),
                replyContent,
                "/post/" + postId + "#comment-" + commentId,
                null
        );
        handleSingleNewInbox(recipient, NoticeType.REPLY, BusinessType.COMMENT, commentId, title, payload);
    }

    @Override
    public void onPostReply(Long recipient, Long userUid, Long commentId, String title, Long postId, String commentContent) {
        if (recipient == null || recipient.equals(userUid)) return;
        if (!isNotificationAllowed(recipient, "comment")) return;
        String coverageResUuid = postRepository.findCoverageResourceUuidById(postId);
        ReplyInboxPayload payload = new ReplyInboxPayload(
                List.of(userUid),
                commentContent,
                "/post/" + postId + "#comment-" + commentId,
                coverageResUuid
        );
        handleSingleNewInbox(recipient, NoticeType.REPLY, BusinessType.POST, commentId, title, payload);
    }

    @Override
    public void onNewFollower(Long recipient, Long followerUid) {
        if (recipient == null || recipient.equals(followerUid)) return;
        if (!isNotificationAllowed(recipient, "follow")) return;
        FollowerInboxPayload payload = new FollowerInboxPayload(
                followerUid,
                "/user/" + followerUid
        );
        handleSingleNewInbox(recipient, NoticeType.NEW_FOLLOWER, BusinessType.NONE, followerUid, "New Follower", payload);
    }

    @Transactional
    @Override
    public void deleteInbox(Long id) {
        Inbox inbox = inboxRepository.findById(id).orElseThrow(
                () -> NotFoundException.of("Inbox not found: " + id)
        );
        if (!inbox.getUser().getUid().equals(UserCtxHolder.getUserUid())) {
            throw new ForbiddenException();
        }
        inbox.setIsDeleted(Boolean.TRUE);
        inboxRepository.save(inbox);
    }

    private void handleSingleNewInbox(Long recipient, NoticeType noticeType, BusinessType type,
                                       Serializable targetId, String title, InboxPayload payload) {
        Inbox inbox = new Inbox();
        inbox.setUser(userRepository.getReferenceById(recipient));
        inbox.setNoticeType(noticeType);
        inbox.setBusinessType(type);
        inbox.setTargetId(targetId.toString());
        inbox.setTitle(title);
        inbox.setContent(payload.toMap());
        inboxRepository.save(inbox);
        pushInboxToUser(recipient, inbox);
    }

    private void pushInboxToUser(Long recipient, Inbox inbox) {
        try {
            InboxNotificationRes dto = inboxSystemMapper.toDto(inbox);
            batchResolveCoverage(List.of(inbox), List.of(dto));
            boolean pushed = sseService.sendToUser(recipient, dto);
            if (pushed) {
                log.debug("SSE pushed notification to user {}", recipient);
            }
        } catch (Exception e) {
            log.warn("Failed to push SSE notification to user {}: {}", recipient, e.getMessage());
        }
    }

    private void batchResolveCoverage(List<Inbox> inboxes, List<InboxNotificationRes> dtos) {
        Set<String> uuids = new HashSet<>();
        for (Inbox inbox : inboxes) {
            Map<String, Object> content = inbox.getContent();
            if (content == null) continue;
            Object val = content.get("postCoverageResUuid");
            String uuid = val instanceof String s ? s : (val == null ? null : val.toString());
            if (uuid != null && !uuid.isEmpty()) {
                uuids.add(uuid);
            }
        }
        if (uuids.isEmpty()) return;

        Map<String, String> uuidToKey = new HashMap<>();
        for (Resource r : resourceRepository.findByUuidInAndStatus(uuids, ResourceStatus.ACTIVE)) {
            uuidToKey.put(r.getUuid(), r.getResourceKey());
        }
        if (uuidToKey.isEmpty()) return;

        try {
            Map<String, CloudResPresignedUrlResp> resolved = cloudFileService.batchGetReadPublicUrlCached(
                    CloudFSRoot.UPLOADS, uuidToKey, TargetType.POST_COVERAGE_IMAGE
            );
            for (int i = 0; i < dtos.size(); i++) {
                Map<String, Object> content = inboxes.get(i).getContent();
                if (content == null) continue;
                Object val = content.get("postCoverageResUuid");
                String uuid = val instanceof String s ? s : (val == null ? null : val.toString());
                if (uuid == null || uuid.isEmpty() || !resolved.containsKey(uuid)) continue;
                if (dtos.get(i).getContent() instanceof NotificationCoverageContent coverage) {
                    coverage.setCoveragePresignedUrl(resolved.get(uuid));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to batch resolve coverage images: {}", e.getMessage());
        }
    }
}
