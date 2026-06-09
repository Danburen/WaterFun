package org.waterwood.waterfunservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeContentDTO implements NotificationContent {
    private List<Long> userUids;
    private Long imageUuid;
    private String nativeUrl;

    @Override
    public String getDisplayText() {
        return userUids != null && !userUids.isEmpty()
                ? userUids.size() + " users liked"
                : "";
    }
}
