package org.waterwood.waterfunservicecore.api.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.audit.task.MediaResourceType;
import org.waterwood.waterfunservicecore.entity.audit.AuditRejectType;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationConsumerMessage implements Serializable {
    private Long id;
    private String targetId;
    private MediaResourceType targetType;
    private AuditStatus status;
    private Long submitterId;
    private String userLocale;
    private Instant auditAt;
    private String rejectReason;
    private AuditRejectType rejectType;
}