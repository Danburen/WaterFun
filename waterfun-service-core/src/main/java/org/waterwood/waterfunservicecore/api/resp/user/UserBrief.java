package org.waterwood.waterfunservicecore.api.resp.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.user.UserType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBrief {
    private Long uid;

    private String displayName;
    private CloudResPresignedUrlResp avatar;
    private Short level;
    private UserType userType;
}
