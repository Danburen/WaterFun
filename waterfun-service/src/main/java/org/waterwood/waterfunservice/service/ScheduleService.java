package org.waterwood.waterfunservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunservice.service.account.AccountService;
import org.waterwood.waterfunservicecore.services.audit.UserActivityLogService;
import org.waterwood.waterfunservicecore.services.auth.AuthTokenService;
import org.waterwood.waterfunservicecore.services.auth.DeviceService;
import org.waterwood.waterfunservicecore.services.stats.SiteStatisticRecorder;
import org.waterwood.waterfunservicecore.services.stats.UserLastActiveService;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final AccountService accountService;
    private final AuthTokenService authTokenService;
    private final DeviceService deviceService;
    private final SSEService sseService;
    private final IpBanService ipBanService;
    private final UserLastActiveService userLastActiveService;
    private final SiteStatisticRecorder siteStatisticRecorder;
    private final UserActivityLogService userActivityLogService;

    @Async
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanUnverifiedEmail() {
        accountService.cleanUnverifiedEmail();
    }

    @Async
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanZombieRefFamily() {
        authTokenService.cleanZombieRefFamily();
    }

    @Async
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanZombieDevices() {
        deviceService.scheduledCleanup();
    }

    @Scheduled(fixedRateString = "${sse.heartbeat-interval-ms:30000}")
    public void sseHeartbeat() {
        sseService.heartbeat();
    }

    @Scheduled(fixedRateString = "${ip-ban.refresh-interval-ms:60000}")
    public void refreshIpBanCache() {
        ipBanService.refreshCache();
    }

    @Scheduled(fixedRateString = "${user-last-active.flush-interval-ms:15000}")
    public void flushUserLastActive() {
        userLastActiveService.flush();
    }

    @Scheduled(fixedRateString = "${stat.flush-interval-ms:10000}")
    public void flushSiteStatistics() {
        siteStatisticRecorder.flush();
    }

    @Scheduled(fixedDelayString = "${activity-log.flush-interval-ms:15000}")
    public void flushActivityLogs() {
        userActivityLogService.flushBatch();
    }
}
