package org.waterwood.waterfunservice.api.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.waterwood.waterfunservicecore.entity.audit.AuditType;

@Data
public class CreateReportReq {
    @NotNull
    private AuditType type;
    private String reason;

    /**
     * 选择"其他"原因时必须补充说明，预设原因可以不填内容。
     */
    @AssertTrue(message = "选择其他原因时必须补充说明")
    public boolean isReasonValid() {
        return type != AuditType.OTHER || (reason != null && !reason.isBlank());
    }
}
