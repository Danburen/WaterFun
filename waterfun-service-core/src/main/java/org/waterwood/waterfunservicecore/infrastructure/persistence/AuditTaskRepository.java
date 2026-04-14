package org.waterwood.waterfunservicecore.infrastructure.persistence;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.waterwood.waterfunservicecore.entity.AuditTask;
import org.waterwood.waterfunservicecore.entity.AuditTaskStatus;

public interface AuditTaskRepository extends JpaRepository<AuditTask, Long> {
    AuditTask findByTargetIdAndStatus(@Size(max = 64) @NotNull String targetId, @NotNull AuditTaskStatus status);
}
