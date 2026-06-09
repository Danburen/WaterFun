package org.waterwood.waterfunservicecore.services.stats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.waterfunservicecore.entity.SiteStatistic;
import org.waterwood.waterfunservicecore.infrastructure.persistence.SiteStatisticRepository;

import java.time.Instant;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteStatisticRecorder {

    private final SiteStatisticRepository repository;

    @Transactional
    public void recordLogin() {
        incrementOrCreate(repository::incrementLoginCount, stat -> {
            stat.setLoginCount(1L);
            return stat;
        });
    }

    @Transactional
    public void recordNewUser() {
        incrementOrCreate(repository::incrementNewUsers, stat -> {
            stat.setNewUsers(1L);
            return stat;
        });
    }

    @Transactional
    public void recordNewPost() {
        incrementOrCreate(repository::incrementNewPosts, stat -> {
            stat.setNewPosts(1L);
            return stat;
        });
    }

    @Transactional
    public void recordPageView() {
        incrementOrCreate(repository::incrementDailyPv, stat -> {
            stat.setDailyPv(1L);
            return stat;
        });
    }

    @Transactional
    public void recordPeakOnline(long currentCount) {
        LocalDate today = LocalDate.now();
        Instant now = Instant.now();
        int updated = repository.updatePeakOnline(today, currentCount, now);
        if (updated == 0) {
            try {
                SiteStatistic stat = new SiteStatistic();
                stat.setId(today);
                stat.setPeakOnline(currentCount);
                stat.setUpdatedAt(now);
                repository.save(stat);
            } catch (DataIntegrityViolationException e) {
                repository.updatePeakOnline(today, currentCount, now);
            }
        }
    }

    private void incrementOrCreate(
            java.util.function.BiFunction<LocalDate, Instant, Integer> incrementFn,
            java.util.function.Function<SiteStatistic, SiteStatistic> initializer
    ) {
        LocalDate today = LocalDate.now();
        Instant now = Instant.now();
        int updated = incrementFn.apply(today, now);
        if (updated == 0) {
            try {
                SiteStatistic stat = initializer.apply(new SiteStatistic());
                stat.setId(today);
                stat.setUpdatedAt(now);
                repository.save(stat);
            } catch (DataIntegrityViolationException e) {
                incrementFn.apply(today, now);
            }
        }
    }
}
