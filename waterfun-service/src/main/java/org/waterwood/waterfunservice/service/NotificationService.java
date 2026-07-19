package org.waterwood.waterfunservice.service;

import org.waterwood.api.VO.BatchResult;
import org.waterwood.waterfunservice.api.request.notifications.BatchMarkReadReq;
import org.waterwood.waterfunservice.api.response.InboxNotificationRes;
import org.waterwood.waterfunservice.infrastructure.dto.MultiUserIncludedInboxPayload;
import org.waterwood.waterfunservice.infrastructure.dto.ReactionInboxPayload;
import org.waterwood.waterfunservicecore.api.CursorPage;
import org.waterwood.waterfunservicecore.entity.notification.BusinessType;
import org.waterwood.waterfunservicecore.entity.notification.NoticeType;

import java.io.Serializable;
import java.util.List;

public interface NotificationService {
    /**
     * List system notifications for the current user, ordered by createdAt desc.
     *
     * @param cursor     the cursor for pagination, can be null for the first page
     * @param limit      limit for pagination size
     * @param unreadOnly whether to only return unread notifications, if null or false, return all notifications
     * @param types      filter by notice types, if null or empty return all types
     * @return Cursor page of SystemNotificationRes
     */
    CursorPage<InboxNotificationRes, Long> list(Long cursor, Integer limit, Boolean unreadOnly, List<NoticeType> types);

    /**
     * Get unread system notification count.
     * @return unread count
     */
    Integer countAllUnread();

    /**
     * Get unread notification count with per-tab breakdown.
     * @return UnreadCountResp containing total and per-tab counts
     */
    org.waterwood.waterfunservice.api.response.notifications.UnreadCountResp getUnreadCountWithTabs();

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
     * Delete a single notification by id (soft delete).
     * @param id notification id
     */
    void deleteInbox(Long id);


    /**
     * Like notification
     *
     * @param recipient    the recipient uid, if null, will do nothing
     * @param userUid      sender user uid must not null
     * @param type
     * @param businessType business type
     * @param title        title of the inbox message
     * @param payload      {@link ReactionInboxPayload} for the inbox message link
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
    void onPostLike(Long recipient, Long userUid, Long postId, String title, String coverageResourceUuid);

    /**
     * Post collection notification
     * @param recipient             the recipient uid, if null, will do nothing
     * @param userUid               sender user uid must not null
     * @param postId                post id for the collected post
     * @param title                 title of the inbox message
     * @param coverageResourceUuid  post coverage resource uuid
     */
    void onPostCollect(Long recipient, Long userUid, Long postId, String title, String coverageResourceUuid);

    /**
     * Reply notification — notify comment author when someone replies to their comment.
     * @param recipient     the comment author uid, if null, will do nothing
     * @param userUid       replier user uid
     * @param commentId     the reply comment id
     * @param title         parent comment content as title
     * @param postId        post id for link
     * @param replyContent  the reply content snippet
     */
    void onReply(Long recipient, Long userUid, Long commentId, String title, Long postId, String replyContent);

    /**
     * Post reply notification — notify post author when someone comments on their post.
     * @param recipient     the post author uid, if null, will do nothing
     * @param userUid       commenter user uid
     * @param commentId     the comment id
     * @param title         post title as title
     * @param postId        post id for link
     * @param commentContent the comment content snippet
     */
    void onPostReply(Long recipient, Long userUid, Long commentId, String title, Long postId, String commentContent);

    /**
     * New follower notification — notify user when someone follows them.
     * @param recipient     the followed user uid
     * @param followerUid   the follower user uid
     */
    void onNewFollower(Long recipient, Long followerUid);
}
