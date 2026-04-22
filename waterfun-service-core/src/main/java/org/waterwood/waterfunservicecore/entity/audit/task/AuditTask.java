package org.waterwood.waterfunservicecore.entity.audit.task;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.waterwood.waterfunservicecore.entity.audit.AuditRejectType;
import org.waterwood.waterfunservicecore.entity.audit.AuditContentFormat;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
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
    private MediaResourceType targetType = MediaResourceType.UNKNOWN;

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

}