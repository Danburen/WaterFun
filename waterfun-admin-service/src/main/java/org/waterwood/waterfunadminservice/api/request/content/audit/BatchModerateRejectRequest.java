package org.waterwood.waterfunadminservice.api.request.content.audit;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.audit.AuditType;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchModerateRejectRequest {
    @NotNull
    private List<Long> auditTaskIds;
    @NotNull
    private AuditType rejectType;
    private String rejectReason;
}
