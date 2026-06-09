package org.waterwood.waterfunadminservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnlineUserVO {
    private Long uid;
    private UserBrief userBrief;
    private Long lastActive;
    private String sessionId;
}
