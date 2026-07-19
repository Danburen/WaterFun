package org.waterwood.waterfunservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.services.online.OnlineUserService;
import org.waterwood.waterfunservicecore.services.stats.SiteStatisticRecorder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SSEService {

    private final OnlineUserService onlineUserService;
    private final SiteStatisticRecorder siteStatisticRecorder;
    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Value("${sse.emitter-timeout-ms:1800000}")
    private long emitterTimeoutMs;

    public SseEmitter subscribe(Long uid, String ip) {
        SseEmitter old = emitters.remove(uid);
        if (old != null) {
            old.complete();
        }

        SseEmitter emitter = new SseEmitter(emitterTimeoutMs);
        emitters.put(uid, emitter);
        onlineUserService.userOnline(
                uid,
                "sse-" + StringUtil.noDashUUIDString(UUID.randomUUID()),
                ip
        );
        long onlineCount = onlineUserService.getOnlineCount();
        siteStatisticRecorder.recordPeakOnline(onlineCount);

        emitter.onCompletion(() -> {
            emitters.remove(uid);
            onlineUserService.userOffline(uid);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE timeout for user {}", uid);
            emitters.remove(uid);
            onlineUserService.userOffline(uid);
        });

        emitter.onError(e -> {
            log.debug("SSE error for user {}: {}", uid, e.getMessage());
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
            // SSE OFFLINE
            emitters.remove(uid);
            onlineUserService.userOffline(uid);
            return false;
        }
    }

    public void heartbeat() {
        if (emitters.isEmpty()) return;

        List<Long> dead = new ArrayList<>();
        emitters.forEach((uid, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data(""));
            } catch (IOException e) {
                dead.add(uid);
            }
        });

        if (!dead.isEmpty()) {
            log.debug("Heartbeat: removing {} dead SSE connections", dead.size());
            dead.forEach(uid -> {
                emitters.remove(uid);
                onlineUserService.userOffline(uid);
            });
        }
    }
}
