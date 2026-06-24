package org.waterwood.waterfunadminservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunadminservice.api.response.user.UserAdminBrief;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
import org.waterwood.waterfunservicecore.entity.Priority;
import org.waterwood.waterfunservicecore.entity.audit.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditTaskRes {
    private Long taskId;
    private TargetType targetType;
    private AuditTriggerType triggerType;
    private String triggerSource;
    private Priority priority;
    private AuditContentFormat format;
    private AuditStatus status;

    private AuditType rejectType;
    private String rejectReason;

    private Instant submitAt;

    private UserAdminBrief submitter;
    private UserBrief auditor;
}
