package org.waterwood.waterfunservicecore.entity.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBriefDO {
    private Long uid;
    private String username;
    private String nickname;
    private String avatarResourceKey;
    private Short level;
    private UserType userType;

    public String getDisplayName() {
        return nickname == null ? username : nickname;
    }
}
