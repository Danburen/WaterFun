package org.waterwood.waterfunservicecore.infrastructure.persistence.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.resource.AuditResource;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AuditTaskResourceRepository extends JpaRepository<AuditResource, Long>, JpaSpecificationExecutor<AuditResource> {
    void deleteByTaskId(Long taskId);

    List<AuditResource> findAllByTask_IdOrderBySortNoAsc(Long taskId);

    List<AuditResource> findAllByTask_IdInOrderBySortNoAsc(Collection<Long> taskIds);

    Optional<AuditResource> findByIdAndStatus(Long id, AuditStatus status);

    long countByTask_IdAndStatus(Long taskId, AuditStatus status);

    Optional<AuditResource> findByTaskId(Long taskId);
}