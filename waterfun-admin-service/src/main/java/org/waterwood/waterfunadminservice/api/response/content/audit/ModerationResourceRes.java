package org.waterwood.waterfunadminservice.api.response.content.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.audit.AuditRejectType;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.audit.resource.AuditResourceType;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResourceRes {
    private Long id;
    private Long taskId;
    private String placeholder;
    private String resourceKey;
    private AuditResourceType resourceType;
    private String mimeType;
    private Long sizeBytes;
    private Long sortNo;
    private AuditStatus status;
    private Instant auditAt;
    private Long auditorId;
    private AuditRejectType rejectType;
    private String rejectReason;
    private CloudResPresignedUrlResp presignedUrl;
}

