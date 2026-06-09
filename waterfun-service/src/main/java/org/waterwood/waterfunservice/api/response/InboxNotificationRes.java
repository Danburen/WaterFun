package org.waterwood.waterfunservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.notification.Inbox;

import java.time.Instant;

/**
 * DTO for {@link Inbox}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InboxNotificationRes {
    private Long id;
    private String title;
    private Short noticeType;
    private NotificationContent content;
    private Instant createdAt;
    private Boolean isRead;
}