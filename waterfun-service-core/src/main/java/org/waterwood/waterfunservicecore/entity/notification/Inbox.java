package org.waterwood.waterfunservicecore.entity.notification;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;
import org.waterwood.waterfunservicecore.entity.Priority;
import org.waterwood.waterfunservicecore.entity.user.User;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "inbox")
public class Inbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_uid", nullable = false)
    private User user;

    @Column(name = "notice_type", columnDefinition = "tinyint UNSIGNED not null")
    private NoticeType noticeType = NoticeType.GENERAL;

    @Column(name = "business_type", columnDefinition = "tinyint UNSIGNED")
    private BusinessType businessType = BusinessType.NONE;

    @Size(max = 64)
    @Column(name = "target_id", length = 64)
    private String targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Size(max = 64)
    @NotNull
    @Column(name = "title", nullable = false, length = 64)
    private String title;

    @NotNull
    @Column(name = "content", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> content;

    @ColumnDefault("'3'")
    @Column(name = "priority", columnDefinition = "tinyint UNSIGNED")
    private Priority priority = Priority.MEDIUM;

    @ColumnDefault("0")
    @Column(name = "is_aggregated")
    private Boolean isAggregated;

    @ColumnDefault("'1'")
    @Column(name = "aggregate_count", columnDefinition = "int UNSIGNED")
    private Integer aggregateCount = 1;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

}