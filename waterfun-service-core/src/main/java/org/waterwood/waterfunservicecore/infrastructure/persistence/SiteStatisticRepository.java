package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.SiteStatistic;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public interface SiteStatisticRepository extends JpaRepository<SiteStatistic, LocalDate>, JpaSpecificationExecutor<SiteStatistic> {
    Optional<SiteStatistic> findTopByOrderByUpdatedAtDesc();

    @Modifying
    @Query("UPDATE SiteStatistic s SET s.loginCount = s.loginCount + 1, s.updatedAt = :now WHERE s.id = :date")
    int incrementLoginCount(@Param("date") LocalDate date, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE SiteStatistic s SET s.newUsers = s.newUsers + 1, s.updatedAt = :now WHERE s.id = :date")
    int incrementNewUsers(@Param("date") LocalDate date, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE SiteStatistic s SET s.newPosts = s.newPosts + 1, s.updatedAt = :now WHERE s.id = :date")
    int incrementNewPosts(@Param("date") LocalDate date, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE SiteStatistic s SET s.dailyPv = s.dailyPv + 1, s.updatedAt = :now WHERE s.id = :date")
    int incrementDailyPv(@Param("date") LocalDate date, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE SiteStatistic s SET s.peakOnline = GREATEST(s.peakOnline, :count), s.updatedAt = :now WHERE s.id = :date")
    int updatePeakOnline(@Param("date") LocalDate date, @Param("count") long count, @Param("now") Instant now);
}