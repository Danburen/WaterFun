package org.waterwood.waterfunservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.waterwood.waterfunservicecore.services.online.OnlineUserService;
import org.waterwood.waterfunservicecore.services.stats.SiteStatisticRecorder;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SSEService {

    private final OnlineUserService onlineUserService;
    private final SiteStatisticRecorder siteStatisticRecorder;
    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long uid) {
        SseEmitter old = emitters.remove(uid);
        if (old != null) {
            old.complete();
        }

        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(uid, emitter);
        onlineUserService.userOnline(uid, "sse");
        long onlineCount = onlineUserService.getOnlineCount();
        siteStatisticRecorder.recordPeakOnline(onlineCount);

        emitter.onCompletion(() -> {
            emitters.remove(uid);
            onlineUserService.userOffline(uid);
        });

        emitter.onTimeout(() -> {
            emitters.remove(uid);
            onlineUserService.userOffline(uid);
        });

        emitter.onError(e -> {
            emitters.remove(uid);
            onlineUserService.userOffline(uid);
        });

        return emitter;
    }

    public boolean sendToUser(Long uid, Object data) {
        SseEmitter emitter = emitters.get(uid);
        if (emitter == null) {
            return false;
        }
        try {
            emitter.send(SseEmitter.event().name("notification").data(data));
            return true;
        } catch (IOException e) {
            emitters.remove(uid);
            onlineUserService.userOffline(uid);
            log.warn("SSE send failed for user {}, removed connection", uid);
            return false;
        }
    }
}
