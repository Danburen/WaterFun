package org.waterwood.waterfunservicecore.services.online;

public interface OnlineUserService {
    void userOnline(Long uid, String sessionId);
    void userOffline(Long uid);

    long getOnlineCount();
    void renewHeartbeat(Long uid);
}
