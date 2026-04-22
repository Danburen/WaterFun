package org.waterwood.waterfunservicecore.infrastructure.persistence.audit;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.waterwood.waterfunservicecore.entity.audit.task.AuditTask;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;

import java.util.Collection;
import java.util.List;

public interface AuditTaskRepository extends JpaRepository<AuditTask, Long>, JpaSpecificationExecutor<AuditTask> {
    AuditTask findByTargetIdAndStatus(String targetId, AuditStatus status);

    List<AuditTask> findAllByIdInAndStatus(Collection<Long> ids, AuditStatus status);
}
