package org.waterwood.waterfunservicecore.services.stats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelperHolder;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLastActiveService {

    private final RedisHelperHolder redis;
    private final UserRepository userRepository;

    private static final String LAST_ACTIVE_BUFFER = "last_active:buffer";

    public void recordActivity(Long uid) {
        if (uid == null) return;
        redis.hashSet(LAST_ACTIVE_BUFFER, String.valueOf(uid), String.valueOf(Instant.now().toEpochMilli()));
    }

    @Transactional
    public void flush() {
        Map<String, String> buffer = redis.hashGetAll(LAST_ACTIVE_BUFFER);
        if (buffer == null || buffer.isEmpty()) return;

        log.debug("Flushing {} user last_active_at records", buffer.size());

        List<Long> uids = new ArrayList<>();
        Map<Long, Instant> uidToTime = new HashMap<>();
        for (Map.Entry<String, String> entry : buffer.entrySet()) {
            try {
                long uid = Long.parseLong(entry.getKey());
                Instant lastActive = Instant.ofEpochMilli(Long.parseLong(entry.getValue()));
                uids.add(uid);
                uidToTime.put(uid, lastActive);
            } catch (Exception e) {
                log.warn("Failed to parse last_active_at for uid={}: {}", entry.getKey(), e.getMessage());
            }
        }

        if (uids.isEmpty()) return;

        List<User> users = userRepository.findAllById(uids);
        for (User user : users) {
            Instant time = uidToTime.get(user.getUid());
            if (time != null) {
                user.setLastActiveAt(time);
            }
        }
        userRepository.saveAll(users);

        redis.del(LAST_ACTIVE_BUFFER);
    }
}
