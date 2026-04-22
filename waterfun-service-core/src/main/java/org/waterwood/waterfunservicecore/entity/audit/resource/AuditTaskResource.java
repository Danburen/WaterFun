package org.waterwood.waterfunservicecore.entity.audit.resource;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.waterwood.waterfunservicecore.entity.audit.AuditRejectType;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.audit.task.AuditTask;
import org.waterwood.waterfunservicecore.entity.user.User;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "audit_task_resource")
public class AuditTaskResource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "task_id", nullable = false)
    private AuditTask task;

    @Size(max = 64)
    @Column(name = "placeholder", length = 64)
    private String placeholder;

    @Size(max = 255)
    @NotNull
    @Column(name = "resource_key", nullable = false)
    private String resourceKey;

    @Column(name = "resource_type", columnDefinition = "tinyint UNSIGNED not null")
    private AuditResourceType resourceType = AuditResourceType.UNKNOWN;

    @Size(max = 64)
    @Column(name = "mime_type", length = 64)
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @ColumnDefault("'0'")
    @Column(name = "sort_no", columnDefinition = "int UNSIGNED not null")
    private Long sortNo = 0L;

    @ColumnDefault("'1'")
    @Column(name = "status", columnDefinition = "tinyint UNSIGNED not null")
    private AuditStatus status = AuditStatus.PENDING;

    @Column(name = "audit_at")
    private Instant auditAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "auditor")
    private User auditor;

    @Column(name = "reject_type", columnDefinition = "tinyint UNSIGNED")
    private AuditRejectType rejectType;

    @Size(max = 255)
    @Column(name = "reject_reason")
    private String rejectReason;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

}