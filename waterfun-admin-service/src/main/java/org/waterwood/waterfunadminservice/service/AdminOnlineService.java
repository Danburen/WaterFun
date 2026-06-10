package org.waterwood.waterfunadminservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunadminservice.api.response.OnlineUserVO;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
import org.waterwood.waterfunservicecore.entity.user.UserType;
import org.waterwood.waterfunservicecore.services.online.OnlineUserService;
import org.waterwood.waterfunservicecore.services.user.UserBriefService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminOnlineService {

    private final OnlineUserService onlineUserService;
    private final UserBriefService userBriefService;

    private static final String FIELD_LAST_ACTIVE = "lastActive";
    private static final String FIELD_SESSION_ID = "sessionId";
    private static final String FIELD_IP = "ip";

    public Page<OnlineUserVO> listOnlineUsers(int page, int size,
                                              String keyword, UserType userType,
                                              Short levelMin, Short levelMax) {
        if (keyword != null || userType != null || levelMin != null || levelMax != null) {
            return listOnlineUsersWithFilter(page, size, keyword, userType, levelMin, levelMax);
        }

        Page<Long> uidPage = onlineUserService.listOnlineUserIdsPage(page, size);
        List<Long> uids = uidPage.getContent();
        if (uids.isEmpty()) {
            return new PageImpl<>(List.of(), PageRequest.of(page, size), uidPage.getTotalElements());
        }
        Map<Long, UserBrief> briefMap = userBriefService.queryForMapUserIdBriefMap(uids);
        List<OnlineUserVO> items = buildItems(uids, briefMap);
        return new PageImpl<>(items, PageRequest.of(page, size), uidPage.getTotalElements());
    }

    private Page<OnlineUserVO> listOnlineUsersWithFilter(int page, int size,
                                                          String keyword, UserType userType,
                                                          Short levelMin, Short levelMax) {
        Set<String> allIds = onlineUserService.getOnlineUserIds();
        if (allIds == null || allIds.isEmpty()) {
            return Page.empty();
        }
        List<Long> allUids = allIds.stream().map(Long::valueOf).sorted().toList();
        Map<Long, UserBrief> briefMap = userBriefService.queryForMapUserIdBriefMap(allUids);

        List<Long> filtered = allUids.stream()
                .filter(uid -> {
                    UserBrief brief = briefMap.get(uid);
                    if (brief == null) return false;
                    if (keyword != null && !keyword.isBlank()) {
                        String kw = keyword.toLowerCase();
                        if (!brief.getDisplayName().toLowerCase().contains(kw)
                                && !String.valueOf(uid).contains(kw)) return false;
                    }
                    if (userType != null && brief.getUserType() != userType) return false;
                    if (levelMin != null && (brief.getLevel() == null || brief.getLevel() < levelMin)) return false;
                    if (levelMax != null && (brief.getLevel() == null || brief.getLevel() > levelMax)) return false;
                    return true;
                })
                .toList();

        int total = filtered.size();
        int fromIndex = page * size;
        if (fromIndex >= total) {
            return new PageImpl<>(List.of(), PageRequest.of(page, size), total);
        }
        int toIndex = Math.min(fromIndex + size, total);
        List<Long> pageUids = filtered.subList(fromIndex, toIndex);

        List<OnlineUserVO> items = buildItems(pageUids, briefMap);
        return new PageImpl<>(items, PageRequest.of(page, size), total);
    }

    private List<OnlineUserVO> buildItems(List<Long> uids, Map<Long, UserBrief> briefMap) {
        return uids.stream().map(uid -> {
            Map<String, String> info = onlineUserService.getUserOnlineInfo(uid);
            String lastActiveStr = info.get(FIELD_LAST_ACTIVE);
            String sessionId = info.get(FIELD_SESSION_ID);
            String ip = info.get(FIELD_IP);
            UserBrief brief = briefMap.get(uid);
            return new OnlineUserVO(
                    uid,
                    brief,
                    StringUtil.isBlank(lastActiveStr) ? null : Instant.ofEpochMilli(Long.parseLong(lastActiveStr)),
                    sessionId,
                    ip
            );
        }).toList();
    }

    public void forceOffline(Long uid) {
        onlineUserService.userOffline(uid);
    }

    public long getOnlineCount() {
        return onlineUserService.getOnlineCount();
    }

    public long getAdminOnlineCount() {
        Set<String> allIds = onlineUserService.getOnlineUserIds();
        if (allIds == null || allIds.isEmpty()) return 0;
        List<Long> uids = allIds.stream().map(Long::valueOf).toList();
        Map<Long, UserBrief> briefMap = userBriefService.queryForMapUserIdBriefMap(uids);
        return briefMap.values().stream()
                .filter(b -> b != null && b.getUserType() == UserType.ADMIN)
                .count();
    }
}
