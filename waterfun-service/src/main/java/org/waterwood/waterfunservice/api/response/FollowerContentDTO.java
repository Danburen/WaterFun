package org.waterwood.waterfunservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowerContentDTO implements NotificationContent {
    private Long followerUid;
    private String nativeUrl;

    @Override
    public String getDisplayText() {
        return "New follower";
    }
}
