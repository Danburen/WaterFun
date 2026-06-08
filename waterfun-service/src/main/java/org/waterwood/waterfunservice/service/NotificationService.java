package org.waterwood.waterfunservice.service;

import org.waterwood.api.VO.BatchResult;
import org.waterwood.waterfunservice.api.response.InboxNotificationRes;
import org.waterwood.waterfunservice.infrastructure.dto.InterationInboxPayload;
import org.waterwood.waterfunservice.api.request.notifications.BatchMarkReadReq;
import org.waterwood.waterfunservice.infrastructure.dto.MultiUserIncludedInboxPayload;
import org.waterwood.waterfunservicecore.api.CursorPage;
import org.waterwood.waterfunservicecore.entity.notification.BusinessType;
import org.waterwood.waterfunservicecore.entity.notification.NoticeType;

import java.io.Serializable;

public interface NotificationService {
    /**
     * List system notifications for the current user, ordered by createdAt desc.
     *
     * @param cursor     the cursor for pagination, can be null for the first page
     * @param limit      limit for pagination size
     * @param unreadOnly whether to only return unread notifications, if null or false, return all notifications
     * @return Cursor page of SystemNotificationRes
     */
    CursorPage<InboxNotificationRes, Long> list(Long cursor, Integer limit, Boolean unreadOnly);

    /**
     * Get unread system notification count.
     * @return unread count
     */
    Integer countAllUnread();

    /**
     * Mark a system notification as read by id.
     * <b>THIS METHOD IS IDEMPOTENT</b>
     * @param id target read
     */
    void markInboxRead(Long id);

    /**
     * Mark all system notification as read
     */
    void markSystemNotificationAllRead();

    /**
     * Batch mark all system notification read
     * @param req request body
     * @return batch result
     */
    BatchResult batchMarkSystemNotificationRead(BatchMarkReadReq req);


    /**
     * Like notification
     *
     * @param recipient    the recipient uid, if null, will do nothing
     * @param userUid      sender user uid must not null
     * @param type
     * @param businessType business type
     * @param title        title of the inbox message
     * @param payload      {@link InterationInboxPayload} for the inbox message link
     */
    void handleAggregateNewInbox(Long recipient, Long userUid, NoticeType type, BusinessType businessType,
                                 Serializable targetBzId, String title, MultiUserIncludedInboxPayload payload);

    /**
     * Comment like notification
     * @param recipient     the recipient uid, if null, will do nothing
     * @param userUid       sender user uid must not nul
     * @param commentId     comment id for the liked comment
     * @param title         title of the inbox message
     * @param postId        post id for the liked comment, used for inbox message link
     */
    void onCommentLike(Long recipient, Long userUid, Long commentId, String title, Long postId);

    /**
     * Post like notification
     * @param recipient     the recipient uid, if null, will do nothing
     * @param userUid       sender user uid must not null
     * @param postId        post id for the liked post
     * @param title         title of the inbox message
     * @param coverageResourceUuid post coverage resource uuid
     */
    void onPostLike(Long recipient, Long userUid, Long postId, String title, Long coverageResourceUuid);

    /**
     * Post collection notification
     * @param recipient             the recipient uid, if null, will do nothing
     * @param userUid               sender user uid must not null
     * @param postId                post id for the collected post
     * @param title                 title of the inbox message
     * @param coverageResourceUuid  post coverage resource uuid, used for inbox message link
     */
    void onPostCollect(Long recipient, Long userUid, Long postId, String title, Long coverageResourceUuid);
}
