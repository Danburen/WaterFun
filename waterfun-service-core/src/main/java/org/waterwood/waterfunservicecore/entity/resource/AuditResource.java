package org.waterwood.waterfunservicecore.entity.resource;

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
import org.waterwood.waterfunservicecore.entity.audit.AuditTask;
import org.waterwood.waterfunservicecore.entity.user.User;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "audit_task_resource")
@NamedEntityGraph(
        name = "AuditResource.withAll",
        attributeNodes = {
                @NamedAttributeNode("task"),
                @NamedAttributeNode("resource")
        }
)
public class AuditResource {

    @EmbeddedId
    private AuditResourceId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "resource_uuid", nullable = false, referencedColumnName = "uuid"
        ,insertable = false,updatable = false)
    private Resource resource;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "task_id", nullable = false,
        insertable = false, updatable = false)
    private AuditTask task;

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

    @Size(max = 255)
    @Column(name = "suspect_reason")
    private String suspect_reason;
    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}