package org.waterwood.waterfunservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.waterfunservice.api.response.InboxNotificationRes;
import org.waterwood.waterfunservice.infrastructure.dto.InboxPayload;
import org.waterwood.waterfunservice.infrastructure.dto.InterationInboxPayload;
import org.waterwood.waterfunservice.infrastructure.dto.MultiUserIncludedInboxPayload;
import org.waterwood.waterfunservicecore.entity.notification.BusinessType;
import org.waterwood.waterfunservicecore.entity.notification.Inbox;
import org.waterwood.waterfunservicecore.entity.notification.NoticeType;
import org.waterwood.waterfunservicecore.exception.ForbiddenException;
import org.waterwood.utils.CollectionUtil;
import org.waterwood.waterfunservice.api.request.notifications.BatchMarkReadReq;
import org.waterwood.waterfunservice.infrastructure.mapper.InboxSystemMapper;
import org.waterwood.waterfunservicecore.api.CursorPage;
import org.waterwood.waterfunservicecore.exception.notfound.NotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.notification.InboxRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;

import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final InboxRepository inboxRepository;
    private final InboxSystemMapper inboxSystemMapper;
    private final UserRepository userRepository;

    @Override
    public CursorPage<InboxNotificationRes, Long> list(Long cursor, Integer limit, Boolean unreadOnly) {
        limit = Math.min(limit, 20);
        List<Inbox> list = inboxRepository.findByCursor(
                UserCtxHolder.getUserUid(),
                cursor,
                unreadOnly,
                limit + 1
        );

        boolean hasNext = list.size() > limit;
        if(hasNext) {
            list = list.subList(0, limit);
        }
        Long nextCursor =  hasNext ? list.getLast().getId() : null;
        return  new CursorPage<>(
                list.stream().map(inboxSystemMapper::toDto).toList(),
                nextCursor,
                hasNext
        );
    }

    @Override
    public Integer countAllUnread() {
        return inboxRepository.countByIsReadAndUserUidAndIsDeleted(Boolean.FALSE, UserCtxHolder.getUserUid(), false);
    }

    @Override
    public void markInboxRead(Long id) {
        Inbox is = inboxRepository.findById(id).orElseThrow(
                () -> NotFoundException.of("InboxSystem id not found: " + id)
        );

        if(! is.getUser().getUid().equals(UserCtxHolder.getUserUid())) {
            throw new ForbiddenException();
        }
        is.setIsRead(Boolean.TRUE);
    }

    @Override
    public void markSystemNotificationAllRead() {
        inboxRepository.markAllReadByUserUid(UserCtxHolder.getUserUid(), Instant.now());
    }

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
            handleSingleNewInbox(recipient, businessType, targetId, title, payload);
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
        }
    }

    @Override
    public void onCommentLike(Long recipient, Long userUid, Long commentId, String title, Long postId) {
        InterationInboxPayload payload = new InterationInboxPayload(
                List.of(userUid),
                null,
                "/post/" + postId + "#comment-" + commentId
        );
        handleAggregateNewInbox(recipient, userUid, NoticeType.LIKE, BusinessType.COMMENT, commentId, title, payload);
    }

    @Override
    public void onPostLike(Long recipient, Long userUid, Long postId, String title, Long coverageResourceUuid) {
        InterationInboxPayload payload = new InterationInboxPayload(
                List.of(userUid),
                coverageResourceUuid,
                "/post/" + postId
        );
        handleAggregateNewInbox(recipient, userUid, NoticeType.LIKE, BusinessType.POST, postId, title, payload);
    }

    @Override
    public void onPostCollect(Long recipient, Long userUid, Long postId, String title, Long coverageResourceUuid) {
        InterationInboxPayload payload = new InterationInboxPayload(
                List.of(userUid),
                coverageResourceUuid,
                "/post/" + postId
        );
        handleAggregateNewInbox(recipient, userUid, NoticeType.COLLECT, BusinessType.POST, postId, title, payload);
    }

    private void handleSingleNewInbox(Long recipient, BusinessType type, Serializable targetId, String title,InboxPayload payload) {
        Inbox inbox = new Inbox();
        inbox.setUser(userRepository.getReferenceById(recipient));
        inbox.setNoticeType(NoticeType.LIKE);
        inbox.setBusinessType(type);
        inbox.setTargetId(targetId.toString());

        inbox.setTitle(title);
        inbox.setContent(payload.toMap());

        inboxRepository.save(inbox);
    }
}
