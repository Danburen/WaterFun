package org.waterwood.waterfunadminservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.Priority;
import org.waterwood.waterfunservicecore.entity.audit.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditTaskRes {
    private Long taskId;
    private Long submitterId;
    private AuditTriggerType triggerType;
    private String triggerSource;
    private Priority priority;
    private AuditContentFormat format;
    private AuditStatus status;

    private AuditRejectType rejectType;
    private String rejectReason;

    private Long submitterUid;
    private Instant submitAt;
}
