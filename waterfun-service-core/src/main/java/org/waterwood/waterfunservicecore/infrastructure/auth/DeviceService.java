package org.waterwood.waterfunservicecore.infrastructure.auth;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

public interface DeviceService {
    String generateAndStoreDeviceId(Long userUid, String dfp);

    void removeUserDevice(Long userUid, String deviceId);

    String generateDeviceId(long userUid, String dfp);

    @Async
    void cleanZombieDevicesBatch(int batchSize);

    @Scheduled(cron = "0 0 3 * * *")
    void scheduledCleanup();

    String getDeviceHashSalt();

    /**
     * Get user's devices
     * @param userUid the user ID
     * @return List of device IDs
     */
    List<String> getUserDeviceIds(Long userUid);
}
