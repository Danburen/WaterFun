package org.waterwood.waterfunservicecore.infrastructure.persistence.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.waterwood.waterfunservicecore.entity.audit.task.AuditTask;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.audit.task.TargetType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AuditTaskRepository extends JpaRepository<AuditTask, Long>, JpaSpecificationExecutor<AuditTask> {
    Optional<AuditTask> findByTargetIdAndStatus(String targetId, AuditStatus status);

    List<AuditTask> findAllByIdInAndStatus(Collection<Long> ids, AuditStatus status);

    Optional<AuditTask> findByTargetIdAndTargetTypeAndStatus(String targetId, TargetType targetType, AuditStatus status);
}
