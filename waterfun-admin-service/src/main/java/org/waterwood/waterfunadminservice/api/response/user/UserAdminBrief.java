package org.waterwood.waterfunadminservice.api.response.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.user.UserType;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAdminBrief {
    private Long uid;
    private String displayName;
    private CloudResPresignedUrlResp avatar;
    private Short level;
    private UserType userType;
    private Instant registrationDate;
    private long postCount;
    private RiskLevel riskLevel;
}
