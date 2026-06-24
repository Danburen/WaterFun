package org.waterwood.waterfunservicecore.infrastructure.persistence.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.audit.AuditType;
import org.waterwood.waterfunservicecore.entity.audit.AuditTask;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.resource.AuditResource;
import org.waterwood.waterfunservicecore.entity.user.User;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AuditTaskRepository extends JpaRepository<AuditTask, Long>, JpaSpecificationExecutor<AuditTask> {

    long countByStatus(AuditStatus status);

    List<AuditTask> findAllByIdInAndStatus(Collection<Long> ids, AuditStatus status);

    Optional<AuditTask> findByTargetIdAndTargetTypeAndStatus(String targetId, TargetType targetType, AuditStatus status);

    Optional<AuditTask> findByTargetIdAndTargetTypeAndSubmitterUidAndStatus(String targetId, TargetType targetType, Long submitterUid, AuditStatus status);

    @Query("UPDATE AuditTask a SET a.status = :status, a.rejectType = :type, a.rejectReason = :reason, a.auditor = :auditor," +
            "a.auditAt = :auditAt WHERE a.id IN :ids AND a.status = :oldStatus")
    @Modifying
    List<AuditTask> updateStatusAndRejectTypeAndRejectReasonAndAuditorAndAuditAtByIdInAndStatus(
            @Param("status") AuditStatus status,
            @Param("type") AuditType rejectType,
            @Param("reason") String reason,
            @Param("auditor") User auditor,
            @Param("auditAt") Instant auditAt,
            @Param("ids") Collection<Long> ids,
            @Param("oldStatus") AuditStatus oldStatus
    );

    @Query("UPDATE AuditTask a SET a.status = :status, a.auditor = :auditor," +
            "a.auditAt = :auditAt WHERE a.id IN :ids AND a.status = :oldStatus")
    @Modifying
    List<AuditTask> updateStatusAndAuditorAndAuditAtByIdInAndStatus(
            @Param("status") AuditStatus status,
            @Param("auditor") User auditor,
            @Param("auditAt") Instant auditAt,
            @Param("ids") Collection<Long> ids,
            @Param("oldStatus") AuditStatus oldStatus
    );

    Optional<AuditTask> findByIdAndStatus(Long id, AuditStatus status);

    long countByStatusAndTargetType(AuditStatus status, TargetType targetType);

    long countByStatusAndAuditAtAfter(AuditStatus status, Instant after);

    long countByStatusAndTargetTypeAndAuditAtAfter(AuditStatus status, TargetType targetType, Instant after);

    long countBySubmitterUidAndStatus(Long submitterUid, AuditStatus status);
}
