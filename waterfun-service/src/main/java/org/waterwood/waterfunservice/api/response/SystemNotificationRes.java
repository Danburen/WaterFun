package org.waterwood.waterfunservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for {@link org.waterwood.waterfunservicecore.entity.notification.InboxSystem}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SystemNotificationRes {
    private String title;
    private String content;
    private Instant createdAt;
    private Boolean isRead;
}