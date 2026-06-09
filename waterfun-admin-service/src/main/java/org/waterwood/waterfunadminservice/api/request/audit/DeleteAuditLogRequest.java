package org.waterwood.waterfunadminservice.api.request.audit;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAuditLogRequest {
    @NotEmpty
    @Size(max = 1000)
    private List<Long> logIds;
}
