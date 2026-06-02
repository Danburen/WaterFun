package org.waterwood.waterfunadminservice.api.response.content.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.common.io.FileProbeResult;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.audit.AuditRejectType;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResourceRes {
    private Long taskId;
    private String resourceUuid;
    private AuditStatus status;
    private Instant auditAt;
    private Long auditorId;
    private AuditRejectType rejectType;
    private String rejectReason;
    private FileProbeResult fileProbeResult;
    private CloudResPresignedUrlResp presignedUrl;
}

