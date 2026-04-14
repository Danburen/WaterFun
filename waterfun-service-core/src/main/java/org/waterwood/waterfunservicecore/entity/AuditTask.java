package org.waterwood.waterfunservicecore.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "audit_task")
public class AuditTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ColumnDefault("'TEXT'")
    @Lob
    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private TaskType taskType = TaskType.TEXT;

    @Size(max = 64)
    @NotNull
    @Column(name = "target_id", nullable = false, length = 64)
    private String targetId;

    @NotNull
    @ColumnDefault("'PENDING'")
    @Lob
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditTaskStatus status = AuditTaskStatus.PENDING;

    @Column(name = "content")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> content;

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

    @Column(name = "auditor")
    private Long auditor;

    @Size(max = 255)
    @Column(name = "reject_reason")
    private String rejectReason;

}