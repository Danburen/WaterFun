package org.waterwood.waterfunservice.api.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.entity.audit.AuditType;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.security.PenaltyType;
import org.waterwood.waterfunservicecore.entity.ticket.TicketType;

import java.util.List;

@Data
public class CreateUserReportReq {
    @NotNull
    private TicketType ticketType;
    private AuditType type;
    private String reason;
    private String targetId;
    private TargetType targetType = TargetType.DEFAULT;
    private List<String> resourceUuids;
    private PenaltyType penaltyType;

    /**
     * Validation: CONTENT_REPORT requires targetId + targetType (not DEFAULT).
     * Appeal, Suggestion, Feedback don't need them.
     */
    @AssertTrue(message = "举报时必须指定目标ID和类型")
    public boolean isTargetValidForReport() {
        if (ticketType != TicketType.CONTENT_REPORT) {
            return true;
        }
        return StringUtil.isNotBlank(targetId)
                && targetType != null
                && targetType != TargetType.DEFAULT;
    }

    /**
     * Validation: reason rules:
     * - CONTENT_REPORT: required only when AuditType is OTHER (选择"其他"时必须补充说明)
     * - SUGGESTION / FEATURE_FEEDBACK / ACCOUNT_APPEAL: reason IS the content, always required
     */
    @AssertTrue(message = "内容不能为空")
    public boolean isReasonValid() {
        if (ticketType == TicketType.CONTENT_REPORT) {
            return type != AuditType.OTHER || StringUtil.isNotBlank(reason);
        }
        return StringUtil.isNotBlank(reason);
    }
}
