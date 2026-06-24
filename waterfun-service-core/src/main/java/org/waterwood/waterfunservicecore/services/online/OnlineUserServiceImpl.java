package org.waterwood.waterfunservicecore.services.online;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelperHolder;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OnlineUserServiceImpl implements OnlineUserService {

    private final RedisHelperHolder redis;

    @Value("${online-user.ttl:300}")
    private long onlineTtl;

    private static final String ONLINE_USERS_KEY = "online:users";
    private static final String ONLINE_USER_PREFIX = "online:user:";
    private static final String FIELD_SESSION_ID = "sessionId";
    private static final String FIELD_LAST_ACTIVE = "lastActive";
    private static final String FIELD_IP = "ip";

    @Override
    public void userOnline(Long uid, String sessionId, String ip) {
        String userKey = ONLINE_USER_PREFIX + uid;
        Map<String, String> fields = new HashMap<>();
        fields.put(FIELD_SESSION_ID, sessionId);
        fields.put(FIELD_LAST_ACTIVE, String.valueOf(System.currentTimeMillis()));
        fields.put(FIELD_IP, ip != null ? ip : "0.0.0.0");
        redis.hashSetMap(userKey, fields, Duration.ofSeconds(onlineTtl));
        redis.setAdd(ONLINE_USERS_KEY, String.valueOf(uid));
    }

    @Override
    public void userOffline(Long uid) {
        redis.del(ONLINE_USER_PREFIX + uid);
        redis.setRemove(ONLINE_USERS_KEY, String.valueOf(uid));
    }

    @Override
    public long getOnlineCount() {
        Set<String> uids = redis.setMembers(ONLINE_USERS_KEY);
        return uids == null ? 0 : uids.size();
    }

    @Override
    public void updateLastActive(Long uid) {
        if (uid == null) return;
        String userKey = ONLINE_USER_PREFIX + uid;
        Map<String, String> fields = redis.hashGetAll(userKey);
        if (fields != null && !fields.isEmpty()) {
            fields.put(FIELD_LAST_ACTIVE, String.valueOf(System.currentTimeMillis()));
            redis.hashSetMap(userKey, fields, Duration.ofSeconds(onlineTtl));
        }
    }

    @Override
    public Long getLastActive(Long uid) {
        String val = redis.hashGet(ONLINE_USER_PREFIX + uid, FIELD_LAST_ACTIVE);
        return val != null ? Long.parseLong(val) : null;
    }

    @Override
    public Map<String, String> getUserOnlineInfo(Long uid) {
        Map<String, String> info = redis.hashGetAll(ONLINE_USER_PREFIX + uid);
        return info != null ? info : Collections.emptyMap();
    }

    @Override
    public Set<String> getOnlineUserIds() {
        return redis.setMembers(ONLINE_USERS_KEY);
    }

    @Override
    public Page<Long> listOnlineUserIdsPage(int page, int size) {
        Set<String> allIds = redis.setMembers(ONLINE_USERS_KEY);
        if (allIds == null || allIds.isEmpty()) {
            return Page.empty();
        }
        List<Long> sortedIds = allIds.stream()
                .map(Long::valueOf)
                .sorted()
                .toList();
        int total = sortedIds.size();
        int fromIndex = page * size;
        if (fromIndex >= total) {
            return new PageImpl<>(List.of(), PageRequest.of(page, size), total);
        }
        int toIndex = Math.min(fromIndex + size, total);
        return new PageImpl<>(sortedIds.subList(fromIndex, toIndex), PageRequest.of(page, size), total);
    }
}
