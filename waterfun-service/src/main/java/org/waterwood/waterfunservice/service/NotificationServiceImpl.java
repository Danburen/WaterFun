package org.waterwood.waterfunservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.common.exceptions.ForbiddenException;
import org.waterwood.utils.CollectionUtil;
import org.waterwood.waterfunservice.api.response.SystemNotificationRes;
import org.waterwood.waterfunservice.api.request.notifications.BatchMarkReadReq;
import org.waterwood.waterfunservice.infrastructure.mapper.InboxSystemMapper;
import org.waterwood.waterfunservicecore.api.CursorPage;
import org.waterwood.waterfunservicecore.entity.notification.InboxSystem;
import org.waterwood.waterfunservicecore.exception.NotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.notification.InboxSystemRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final InboxSystemRepository inboxSystemRepository;
    private final InboxSystemMapper inboxSystemMapper;

    @Override
    public CursorPage<SystemNotificationRes, Long> list(Long cursor, Integer limit, Boolean unreadOnly) {
        limit = Math.min(limit, 20);
        List<InboxSystem> list = inboxSystemRepository.findByCursor(
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
    public Integer systemUnreadCount() {
        return inboxSystemRepository.countByIsReadAndUserUid(Boolean.FALSE, UserCtxHolder.getUserUid());
    }

    @Override
    public void markSystemNotificationRead(Long id) {
        InboxSystem is = inboxSystemRepository.findById(id).orElseThrow(
                () -> NotFoundException.of("InboxSystem id not found: " + id)
        );

        if(! is.getUser().getUid().equals(UserCtxHolder.getUserUid())) {
            throw new ForbiddenException();
        }
        is.setIsRead(Boolean.TRUE);
    }

    @Override
    public void markSystemNotificationAllRead() {
        inboxSystemRepository.markAllReadByUserUid(UserCtxHolder.getUserUid(), Instant.now());
    }

    @Override
    public BatchResult batchMarkSystemNotificationRead(BatchMarkReadReq req) {
        int marked = 0;
        if(CollectionUtil.isNotEmpty(req.getIds())){
            marked = inboxSystemRepository.markReadBatch(req.getIds(), UserCtxHolder.getUserUid(), Instant.now());
        }
        return BatchResult.ofNullable(req.getIds(), marked);
    }
}
