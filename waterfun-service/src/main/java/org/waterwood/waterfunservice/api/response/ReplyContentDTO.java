package org.waterwood.waterfunservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReplyContentDTO implements NotificationContent {
    private Long replierUid;
    private String replyContent;
    private String nativeUrl;

    @Override
    public String getDisplayText() {
        return replyContent;
    }
}
