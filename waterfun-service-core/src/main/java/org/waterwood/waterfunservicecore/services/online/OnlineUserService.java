package org.waterwood.waterfunservicecore.services.online;

import org.springframework.data.domain.Page;

import java.util.Map;
import java.util.Set;

public interface OnlineUserService {
    void userOnline(Long uid, String sessionId, String ip);
    void userOffline(Long uid);

    long getOnlineCount();

    void updateLastActive(Long uid);
    Long getLastActive(Long uid);
    Map<String, String> getUserOnlineInfo(Long uid);

    Set<String> getOnlineUserIds();

    Page<Long> listOnlineUserIdsPage(int page, int size);
}
