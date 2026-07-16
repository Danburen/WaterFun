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
    @Column(name = "login_count", columnDefinition = "int UNSIGNED")
    private Integer loginCount = 0;

    @ColumnDefault("'0'")
    @Column(name = "daily_pv", columnDefinition = "int UNSIGNED")
    private Integer dailyPv = 0;

    @ColumnDefault("'0'")
    @Column(name = "new_users", columnDefinition = "int UNSIGNED")
    private Integer newUsers = 0;

    @ColumnDefault("'0'")
    @Column(name = "new_posts", columnDefinition = "int UNSIGNED")
    private Integer newPosts = 0;

    @ColumnDefault("'0'")
    @Column(name = "peak_online", columnDefinition = "int UNSIGNED")
    private Integer peakOnline = 0;

    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "updated_at")
    private Instant updatedAt;

}