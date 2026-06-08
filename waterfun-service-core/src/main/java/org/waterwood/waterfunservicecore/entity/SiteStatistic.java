package org.waterwood.waterfunservicecore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "site_statistics")
public class SiteStatistic {
    @Id
    @Column(name = "stat_date", nullable = false)
    private LocalDate id;

    @ColumnDefault("'0'")
    @Column(name = "daily_visits", columnDefinition = "int UNSIGNED")
    private Long dailyVisits;

    @ColumnDefault("'0'")
    @Column(name = "daily_uv", columnDefinition = "int UNSIGNED")
    private Long dailyUv;

    @ColumnDefault("'0'")
    @Column(name = "new_users", columnDefinition = "int UNSIGNED")
    private Long newUsers;

    @ColumnDefault("'0'")
    @Column(name = "active_users", columnDefinition = "int UNSIGNED")
    private Long activeUsers;

    @ColumnDefault("'0'")
    @Column(name = "peak_online", columnDefinition = "int UNSIGNED")
    private Long peakOnline;

    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "updated_at")
    private Instant updatedAt;

}