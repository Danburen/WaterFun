package org.waterwood.waterfunservicecore.infrastructure.persistence.audit;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.audit.AuditType;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.audit.AuditTask;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.resource.AuditResource;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.user.User;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AuditTaskResourceRepository extends JpaRepository<AuditResource, Long>, JpaSpecificationExecutor<AuditResource> {
    long countByTask_IdAndStatus(Long taskId, AuditStatus status);
    Optional<AuditResource> findByTaskId(Long taskId);
    Optional<AuditResource> findByTaskIdAndResourceUuid(Long taskId, String resourceUuid);
    Optional<AuditResource> findByTaskIdAndResourceUuidAndStatus(Long taskId, String resourceUuid, AuditStatus status);

    @EntityGraph(attributePaths = {"auditor"})
    List<AuditResource> findAllByTaskId(Long taskId);


    List<AuditResource> findAllByTaskIdIn(Collection<Long> taskIds);

    @Query("UPDATE AuditResource a SET a.status = :status, a.rejectType = :type, a.auditor = :auditor, a.auditAt = :auditAt " +
            "WHERE a.task.id IN :taskIds")
    @Modifying
    void updateStatusAndRejectTypeAndAuditorAndAuditAtByTaskIdIn(
            @Param("status") AuditStatus status,
            @Param("type") AuditType type,
            @Param("auditor") User auditor,
            @Param("auditAt") Instant auditAt,
            @Param("taskIds") Collection<Long> taskIds);

    @Query("UPDATE AuditResource a SET a.status = :status, a.rejectType = :type, a.auditor = :auditor, a.auditAt = :auditAt " +
            "WHERE a.task.id = :taskId AND a.status = :oldStatus")
    @Modifying
    void updateStatusAndRejectTypeAndAuditorAndAuditAtByTaskIdAndStatus(
            @Param("status") AuditStatus status,
            @Param("type") AuditType type,
            @Param("auditor") User auditor,
            @Param("auditAt") Instant auditAt,
            @Param("taskId") Long taskId,
            @Param("oldStatus") AuditStatus oldStatus);

    List<AuditResource> findByTaskIdIn(List<Long> attr0);

    @Query("SELECT ar.resource FROM AuditResource ar JOIN ar.task t " +
            "WHERE t.targetId = :targetId AND t.targetType = :targetType")
    List<Resource> findByTaskTargetIdAndTaskTargetType(
            @Param("targetId") String targetId,
            @Param("targetType") TargetType targetType);
    @Query("SELECT ar.resource.uuid FROM AuditResource ar JOIN ar.task t " +
            "WHERE t.targetId = :targetId AND t.targetType = :targetType")
    List<String> findResourceUuidByTaskTargetIdAndTaskTargetType(
            @Param("targetId") String targetId,
            @Param("targetType") TargetType targetType);

    @Query("SELECT ar.task FROM AuditResource ar WHERE ar.resource.uuid = :resourceUuid AND ar.task.targetType = :targetType")
    List<AuditTask> findTaskByResourceUuidAndTaskTargetType(
            @Param("resourceUuid") String resourceUuid,
            @Param("targetType") TargetType targetType);
}
