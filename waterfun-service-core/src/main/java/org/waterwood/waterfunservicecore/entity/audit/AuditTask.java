package org.waterwood.waterfunservicecore.entity.audit;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.waterwood.waterfunservicecore.entity.user.User;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "audit_task")
public class AuditTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 64)
    @NotNull
    @Column(name = "target_id", nullable = false, length = 64)
    private String targetId;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "submit_at", nullable = false)
    private Instant submitAt = Instant.now();

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "audit_at")
    private Instant auditAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditor")
    private User auditor;

    @Size(max = 255)
    @Column(name = "reject_reason")
    private String rejectReason;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitter", nullable = false)
    private User submitter;

    @Column(name = "reject_type", columnDefinition = "tinyint UNSIGNED")
    private AuditRejectType rejectType;

    @Column(name = "target_type", columnDefinition = "tinyint UNSIGNED not null")
    private TargetType targetType = TargetType.UNKNOWN;

    @ColumnDefault("'0'")
    @Column(name = "content_format", columnDefinition = "tinyint UNSIGNED not null")
    private AuditContentFormat contentFormat = AuditContentFormat.PLAINTEXT;

    @ColumnDefault("'1'")
    @Column(name = "status", columnDefinition = "tinyint UNSIGNED not null")
    private AuditStatus status = AuditStatus.PENDING;

    @Lob
    @Column(name = "content")
    private String content;

    @Size(max = 10)
    @NotNull
    @ColumnDefault("'zh-CN'")
    @Column(name = "user_locale", nullable = false, length = 10)
    private String userLocale = "zh-CN";

    @Column(name = "payload", columnDefinition = "json")
    private String payload;

    @Size(max = 64)
    @ColumnDefault("(case when (`status` = 1) then `target_id` else NULL end)")
    @Column(name = "pending_target_id", length = 64, updatable = false, insertable = false) // the application usually won't use this
    private String pendingTargetId;

}