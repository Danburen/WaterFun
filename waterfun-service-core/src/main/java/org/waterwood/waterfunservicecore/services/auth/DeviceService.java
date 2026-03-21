package org.waterwood.waterfunservicecore.services.auth;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Set;

public interface DeviceService {
    String generateAndStoreDeviceId(Long userUid, String dfp);

    void removeUserDevice(Long userUid, String deviceId);

    String calculaateDid(long userUid, String dfp);

    @Async
    void cleanZombieDevicesBatch(int batchSize);

    @Scheduled(cron = "0 0 3 * * *")
    void scheduledCleanup();

    String getDeviceHashSalt();

    /**
     * Get user's devices
     *
     * @param userUid the user ID
     * @return List of device IDs
     */
    Set<String> getUserDeviceIds(Long userUid);

    void updateUserDeviceActive(long userUid, String did);

    boolean isNewDeviceDid(long userUid, String calculatedHashDid);
}
