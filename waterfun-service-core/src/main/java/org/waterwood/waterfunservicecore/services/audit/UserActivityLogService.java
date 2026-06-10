package org.waterwood.waterfunservicecore.services.audit;

import org.waterwood.waterfunservicecore.entity.audit.UserActionType;
import org.waterwood.waterfunservicecore.entity.audit.UserActivityLog;
import org.waterwood.waterfunservicecore.entity.notification.BusinessType;

import java.util.List;

public interface UserActivityLogService {

    void record(Long userId, UserActionType actionType, BusinessType businessType, Long targetId, String ip);

    void record(Long userId, UserActionType actionType, BusinessType businessType, Long targetId);

    void flushBatch();

    List<UserActivityLog> findRecentActivities(int limit);

    List<UserActivityLog> findRecentActivitiesByUserIds(List<Long> userIds, int limit);

    List<UserActivityLog> findRecentActivitiesByUserId(Long userId, int limit);
}
