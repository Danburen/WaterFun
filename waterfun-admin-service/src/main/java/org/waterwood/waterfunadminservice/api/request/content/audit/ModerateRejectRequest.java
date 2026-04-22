package org.waterwood.waterfunadminservice.api.request.content.audit;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.audit.AuditRejectType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModerateRejectRequest {
    @NotNull
    private AuditRejectType rejectType;
    @Size(max = 255)
    private String rejectReason;
}
