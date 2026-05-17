package org.waterwood.waterfunservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunservice.service.account.AccountService;
import org.waterwood.waterfunservicecore.services.auth.AuthTokenService;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final AccountService accountService;
    private final AuthTokenService authTokenService;

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
}
