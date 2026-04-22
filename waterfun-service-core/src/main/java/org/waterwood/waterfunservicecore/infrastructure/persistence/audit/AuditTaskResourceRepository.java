package org.waterwood.waterfunservicecore.infrastructure.persistence.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.audit.resource.AuditTaskResource;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AuditTaskResourceRepository extends JpaRepository<AuditTaskResource, Long>, JpaSpecificationExecutor<AuditTaskResource> {
    void deleteByTaskId(Long taskId);

    List<AuditTaskResource> findAllByTask_IdOrderBySortNoAsc(Long taskId);

    List<AuditTaskResource> findAllByTask_IdInOrderBySortNoAsc(Collection<Long> taskIds);

    Optional<AuditTaskResource> findByIdAndStatus(Long id, AuditStatus status);

    long countByTask_IdAndStatus(Long taskId, AuditStatus status);

    Optional<AuditTaskResource> findByTaskId(Long taskId);
}