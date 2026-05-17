package org.waterwood.waterfunservice.service;

import org.waterwood.api.VO.BatchResult;
import org.waterwood.waterfunservice.api.response.SystemNotificationRes;
import org.waterwood.waterfunservice.api.request.notifications.BatchMarkReadReq;
import org.waterwood.waterfunservicecore.api.CursorPage;

public interface NotificationService {
    /**
     * List system notifications for the current user, ordered by createdAt desc.
     *
     * @param cursor     the cursor for pagination, can be null for the first page
     * @param limit      limit for pagination size
     * @param unreadOnly whether to only return unread notifications, if null or false, return all notifications
     * @return Cursor page of SystemNotificationRes
     */
    CursorPage<SystemNotificationRes, Long> list(Long cursor, Integer limit, Boolean unreadOnly);

    /**
     * Get unread system notification count.
     * @return unread count
     */
    Integer systemUnreadCount();

    /**
     * Mark a system notification as read by id.
     * <b>THIS METHOD IS IDEMPOTENT</b>
     * @param id target read
     */
    void markSystemNotificationRead(Long id);

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
}
