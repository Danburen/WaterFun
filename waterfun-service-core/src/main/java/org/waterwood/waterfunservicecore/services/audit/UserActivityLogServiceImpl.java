package org.waterwood.waterfunservicecore.services.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.utils.JsonUtil;
import org.waterwood.waterfunservicecore.entity.audit.UserActionType;
import org.waterwood.waterfunservicecore.entity.audit.UserActivityLog;
import org.waterwood.waterfunservicecore.entity.notification.BusinessType;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelperHolder;
import org.waterwood.waterfunservicecore.infrastructure.persistence.audit.UserActivityLogRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.online.OnlineUserService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityLogServiceImpl implements UserActivityLogService {

    private final RedisHelperHolder redis;
    private final UserActivityLogRepository repository;
    private final OnlineUserService onlineUserService;

    private static final String ACTIVITY_LOG_BUFFER_KEY = "activity:log:buffer";

    @Value("${activity-log.flush-batch-size:100}")
    private int flushBatchSize;

    @Override
    public void record(Long userId, UserActionType actionType, BusinessType businessType, Long targetId, String ip) {
        UserActivityLog log = new UserActivityLog();
        log.setUserId(userId);
        log.setActionType(actionType);
        log.setBusinessType(businessType);
        log.setTargetId(targetId);
        log.setIp(ip);
        log.setCreatedAt(Instant.now());

        String json = JsonUtil.toJson(log);
        redis.setAdd(ACTIVITY_LOG_BUFFER_KEY, json);
        onlineUserService.updateLastActive(userId);
    }

    @Override
    public void record(Long userId, UserActionType actionType, BusinessType businessType, Long targetId) {
        record(userId, actionType, businessType, targetId, resolveClientIp());
    }

    @Override
    @Transactional
    public void flushBatch() {
        Set<String> members = redis.setMembers(ACTIVITY_LOG_BUFFER_KEY);
        if (members == null || members.isEmpty()) return;

        List<UserActivityLog> batch = new ArrayList<>();
        for (String json : members) {
            UserActivityLog entry = JsonUtil.fromJson(json, UserActivityLog.class);
            batch.add(entry);
            if (batch.size() >= flushBatchSize) {
                repository.saveAll(batch);
                log.debug("Flushed {} activity logs to database", batch.size());
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            repository.saveAll(batch);
            log.debug("Flushed {} activity logs to database", batch.size());
        }
        redis.del(ACTIVITY_LOG_BUFFER_KEY);
    }

    @Override
    public List<UserActivityLog> findRecentActivities(int limit) {
        return repository.findTop30ByOrderByCreatedAtDesc().stream()
                .limit(limit)
                .toList();
    }

    @Override
    public List<UserActivityLog> findRecentActivitiesByUserIds(List<Long> userIds, int limit) {
        return repository.findLatestByUserIds(userIds, org.springframework.data.domain.PageRequest.of(0, limit));
    }

    @Override
    public List<UserActivityLog> findRecentActivitiesByUserId(Long userId, int limit) {
        return repository.findLatestByUserId(userId, org.springframework.data.domain.PageRequest.of(0, limit));
    }

    private String resolveClientIp() {
        try {
            return UserCtxHolder.getClientIp();
        } catch (Exception e) {
            return "0.0.0.0";
        }
    }
}
