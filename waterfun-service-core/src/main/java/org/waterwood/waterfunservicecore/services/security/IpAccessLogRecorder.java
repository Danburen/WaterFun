package org.waterwood.waterfunservicecore.services.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunservicecore.entity.security.IpAccessLog;
import org.waterwood.waterfunservicecore.infrastructure.persistence.IpAccessLogRepository;
import org.waterwood.waterfunservicecore.services.location.IpLocationService;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Reusable async IP access log recorder.
 * Both admin-service and user-service filters delegate to this service
 * so the async queue, geo lookup, and persistence logic stay in one place.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IpAccessLogRecorder {

    private final IpAccessLogRepository ipAccessLogRepository;
    private final IpLocationService ipLocationService;

    private static final ThreadPoolExecutor ASYNC_LOG = new ThreadPoolExecutor(
            1, 2,
            60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(5000),
            new ThreadPoolExecutor.DiscardPolicy()
    );

    /**
     * Asynchronously record an IP access log entry.
     * This method returns immediately; the DB write happens on a background thread.
     */
    public void record(String ip, Long userUid, String path, String method, short status) {
        ASYNC_LOG.submit(() -> {
            try {
                IpAccessLog logEntry = new IpAccessLog();
                logEntry.setIp(ip);
                logEntry.setRequestPath(path);
                logEntry.setRequestMethod(method);
                logEntry.setHttpStatus(status);
                if (userUid != null) {
                    logEntry.setUserUid(userUid);
                }

                // Geo lookup (cached by IpLocationService)
                Map<String, String> location = ipLocationService.lookup(ip);
                logEntry.setCountry(location.getOrDefault("country", ""));
                logEntry.setProvince(location.getOrDefault("province", ""));
                logEntry.setCity(location.getOrDefault("city", ""));

                ipAccessLogRepository.save(logEntry);
            } catch (Exception e) {
                log.warn("Failed to log IP access: {}", e.getMessage());
            }
        });
    }
}
