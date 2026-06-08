package org.waterwood.waterfunservicecore.api.resp.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPublicCardResp {
    private Long uid;
    private UserBrief userBrief;

    private Long followers;
    private Long followings;
    private Long likeCount;
    private Long postCount;
}
