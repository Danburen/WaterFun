package org.waterwood.waterfunservicecore.services.online;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelperHolder;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    @Override
    public void userOnline(Long uid, String sessionId) {
        String userKey = ONLINE_USER_PREFIX + uid;
        Map<String, String> fields = new HashMap<>();
        fields.put(FIELD_SESSION_ID, sessionId);
        fields.put(FIELD_LAST_ACTIVE, String.valueOf(System.currentTimeMillis()));
        redis.hSetMap(userKey, fields, Duration.ofSeconds(onlineTtl));
        redis.sAdd(ONLINE_USERS_KEY, String.valueOf(uid));
    }

    @Override
    public void userOffline(Long uid) {
        redis.del(ONLINE_USER_PREFIX + uid);
        redis.sRem(ONLINE_USERS_KEY, String.valueOf(uid));
    }

    @Override
    public long getOnlineCount() {
        Set<String> uids = redis.sMem(ONLINE_USERS_KEY);
        return uids == null ? 0 : uids.size();
    }

    @Override
    public void renewHeartbeat(Long uid) {
        String userKey = ONLINE_USER_PREFIX + uid;
        Map<String, String> fields = redis.hGetAll(userKey);
        if (fields != null && !fields.isEmpty()) {
            fields.put(FIELD_LAST_ACTIVE, String.valueOf(System.currentTimeMillis()));
            redis.hSetMap(userKey, fields, Duration.ofSeconds(onlineTtl));
        }
    }
}
