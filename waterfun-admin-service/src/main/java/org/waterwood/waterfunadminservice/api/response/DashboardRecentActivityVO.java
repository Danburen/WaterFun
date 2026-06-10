package org.waterwood.waterfunadminservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
import org.waterwood.waterfunservicecore.entity.audit.UserActionType;
import org.waterwood.waterfunservicecore.entity.notification.BusinessType;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardRecentActivityVO {
    private UserBrief userBrief;
    private Long lastActiveAt;
    private boolean online;
    private UserActionType actionType;
    private BusinessType businessType;
    private Instant actionTime;
    private Long targetId;
    private String description;
}
