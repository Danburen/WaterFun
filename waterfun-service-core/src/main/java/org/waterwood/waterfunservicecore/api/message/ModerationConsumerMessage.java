package org.waterwood.waterfunservicecore.api.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.audit.AuditTriggerType;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.audit.AuditType;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationConsumerMessage implements Serializable {
    /**
     * Task id
     */
    private Long id;
    private String targetId;
    private TargetType targetType;
    private AuditTriggerType triggerType;
    private AuditStatus status;
    private Long submitterId;
    private Instant auditAt;
    private String rejectReason;
    private AuditType rejectType;

    @Builder.Default
    private Instant sendTime = Instant.now();
}