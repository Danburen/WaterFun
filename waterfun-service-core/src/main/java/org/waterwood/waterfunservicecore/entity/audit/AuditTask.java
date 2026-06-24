package org.waterwood.waterfunservicecore.entity.audit;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.waterwood.waterfunservicecore.entity.Priority;
import org.waterwood.waterfunservicecore.entity.user.User;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "audit_task")
public class AuditTask {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 64)
    @Column(name = "target_id", length = 64)
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
    private AuditType rejectType;

    @Column(name = "target_type", columnDefinition = "tinyint UNSIGNED not null")
    private TargetType targetType = TargetType.DEFAULT;

    @ColumnDefault("'0'")
    @Column(name = "format", columnDefinition = "tinyint UNSIGNED not null")
    private AuditContentFormat format = AuditContentFormat.DEFAULT;

    @ColumnDefault("'1'")
    @Column(name = "status", columnDefinition = "tinyint UNSIGNED not null")
    private AuditStatus status = AuditStatus.PENDING;

    @Column(name = "payload", columnDefinition = "json")
    private String payload;

    @Size(max = 64)
    @ColumnDefault("(case when (`status` = 1) then `target_id` else NULL end)")
    @Column(name = "pending_target_id", length = 64, updatable = false, insertable = false) // the application usually won't use this
    private String pendingTargetId;

    @Column(name = "trigger_type", columnDefinition = "tinyint UNSIGNED not null")
    private AuditTriggerType triggerType = AuditTriggerType.UNKNOWN;

    @Size(max = 255)
    @Column(name = "trigger_source")
    private String triggerSource;

    @ColumnDefault("'2'")
    @Convert(disableConversion = true)
    @Enumerated
    @Column(name = "priority", columnDefinition = "tinyint UNSIGNED not null")
    private Priority priority = Priority.MEDIUM;

}