package org.waterwood.waterfunservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.user.UserType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBrief {
    private String displayName;
    private CloudResPresignedUrlResp avatar;
    private Short level;
    private UserType userType;
}
