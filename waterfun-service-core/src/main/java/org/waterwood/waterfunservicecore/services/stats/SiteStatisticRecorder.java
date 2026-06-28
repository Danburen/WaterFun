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
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteStatisticRecorder {

    private final SiteStatisticRepository repository;

    private volatile StatsDelta delta = new StatsDelta(LocalDate.now());

    public void recordLogin() {
        delta.loginCount.incrementAndGet();
    }

    public void recordNewUser() {
        delta.newUsers.incrementAndGet();
    }

    public void recordNewPost() {
        delta.newPosts.incrementAndGet();
    }

    public void recordPageView() {
        delta.dailyPv.incrementAndGet();
    }

    public void recordPeakOnline(long count) {
        delta.peakOnline.updateAndGet(prev -> Math.max(prev, count));
    }

    public long getCachedNewUsers() {
        return delta.newUsers.get();
    }

    public long getCachedPeakOnline() {
        return delta.peakOnline.get();
    }

    public long getTodayNewUsers() {
        SiteStatistic todayStat = repository.findById(LocalDate.now()).orElse(null);
        long dbNewUsers = todayStat != null && todayStat.getNewUsers() != null ? todayStat.getNewUsers() : 0L;
        return dbNewUsers + delta.newUsers.get();
    }

    public long getTodayPeakOnline() {
        SiteStatistic todayStat = repository.findById(LocalDate.now()).orElse(null);
        long dbPeakOnline = todayStat != null && todayStat.getPeakOnline() != null ? todayStat.getPeakOnline() : 0L;
        return Math.max(dbPeakOnline, delta.peakOnline.get());
    }

    @Transactional
    public void flush() {
        StatsDelta old = delta;
        delta = new StatsDelta(LocalDate.now());

        long loginCount = old.loginCount.getAndSet(0);
        long newUsers = old.newUsers.getAndSet(0);
        long newPosts = old.newPosts.getAndSet(0);
        long dailyPv = old.dailyPv.getAndSet(0);
        long peakOnline = old.peakOnline.getAndSet(-1);

        if (loginCount == 0 && newUsers == 0 && newPosts == 0 && dailyPv == 0 && peakOnline < 0) {
            return;
        }

        Instant now = Instant.now();
        LocalDate date = old.date;

        if (loginCount > 0) {
            applyDelta(date, now, loginCount, repository::incrementLoginCount, SiteStatistic::setLoginCount);
        }
        if (newUsers > 0) {
            applyDelta(date, now, newUsers, repository::incrementNewUsers, SiteStatistic::setNewUsers);
        }
        if (newPosts > 0) {
            applyDelta(date, now, newPosts, repository::incrementNewPosts, SiteStatistic::setNewPosts);
        }
        if (dailyPv > 0) {
            applyDelta(date, now, dailyPv, repository::incrementDailyPv, SiteStatistic::setDailyPv);
        }
        if (peakOnline >= 0) {
            int updated = repository.updatePeakOnline(date, peakOnline, now);
            if (updated == 0) {
                try {
                    SiteStatistic stat = new SiteStatistic();
                    stat.setId(date);
                    stat.setUpdatedAt(now);
                    stat.setPeakOnline(peakOnline);
                    repository.save(stat);
                } catch (DataIntegrityViolationException e) {
                    repository.updatePeakOnline(date, peakOnline, now);
                }
            }
        }
    }

    private void applyDelta(LocalDate date, Instant now, long delta,
                            IncrementFunction incrementFn,
                            java.util.function.BiConsumer<SiteStatistic, Long> setter) {
        int updated = incrementFn.apply(date, delta, now);
        if (updated == 0) {
            try {
                SiteStatistic stat = new SiteStatistic();
                stat.setId(date);
                stat.setUpdatedAt(now);
                setter.accept(stat, delta);
                repository.save(stat);
            } catch (DataIntegrityViolationException e) {
                incrementFn.apply(date, delta, now);
            }
        }
    }

    @FunctionalInterface
    private interface IncrementFunction {
        int apply(LocalDate date, long delta, Instant now);
    }

    private static class StatsDelta {
        final LocalDate date;
        final AtomicLong loginCount = new AtomicLong(0);
        final AtomicLong newUsers = new AtomicLong(0);
        final AtomicLong newPosts = new AtomicLong(0);
        final AtomicLong dailyPv = new AtomicLong(0);
        final AtomicLong peakOnline = new AtomicLong(-1);

        StatsDelta(LocalDate date) {
            this.date = date;
        }
    }
}
