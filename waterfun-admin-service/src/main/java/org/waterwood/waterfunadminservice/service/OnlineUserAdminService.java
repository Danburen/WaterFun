package org.waterwood.waterfunadminservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunadminservice.api.response.OnlineUserVO;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
import org.waterwood.waterfunservicecore.services.user.UserBriefService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineUserAdminService {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserBriefService userBriefService;

    private static final String ONLINE_USERS_KEY = "online:users";
    private static final String ONLINE_USER_PREFIX = "online:user:";
    private static final String FIELD_LAST_ACTIVE = "lastActive";
    private static final String FIELD_SESSION_ID = "sessionId";

    public Page<OnlineUserVO> listOnlineUsers(int page, int size) {
        long total = Optional.ofNullable(stringRedisTemplate.opsForSet().size(ONLINE_USERS_KEY)).orElse(0L);

        List<String> allUids = new ArrayList<>();
        try (Cursor<String> cursor = stringRedisTemplate.opsForSet().scan(ONLINE_USERS_KEY,
                ScanOptions.scanOptions().count(1000).build())) {
            cursor.forEachRemaining(allUids::add);
        }

        int fromIndex = page * size;
        if (fromIndex >= allUids.size()) {
            return new PageImpl<>(List.of(), PageRequest.of(page, size), total);
        }
        int toIndex = Math.min(fromIndex + size, allUids.size());
        List<String> pageUids = allUids.subList(fromIndex, toIndex);

        List<Long> uidLongs = pageUids.stream().map(Long::valueOf).toList();
        Map<Long, UserBrief> briefMap = userBriefService.queryForMapUserIdBriefMap(uidLongs);

        List<OnlineUserVO> items = pageUids.stream().map(uidStr -> {
            Long uid = Long.valueOf(uidStr);
            String hashKey = ONLINE_USER_PREFIX + uid;
            String lastActiveStr = (String) stringRedisTemplate.opsForHash().get(hashKey, FIELD_LAST_ACTIVE);
            String sessionId = (String) stringRedisTemplate.opsForHash().get(hashKey, FIELD_SESSION_ID);
            Long lastActive = lastActiveStr != null ? Long.parseLong(lastActiveStr) : null;
            return new OnlineUserVO(uid, briefMap.get(uid), lastActive, sessionId);
        }).toList();

        return new PageImpl<>(items, PageRequest.of(page, size), total);
    }

    public long getOnlineCount() {
        Long count = stringRedisTemplate.opsForSet().size(ONLINE_USERS_KEY);
        return count != null ? count : 0L;
    }
}
