package org.waterwood.waterfunservicecore.api.moderation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.common.io.FileMeta;
import org.waterwood.utils.JsonUtil;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.audit.AuditContentFormat;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.SourceType;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageAuditPayload implements AuditPayload {
    private String uuid;
    private String resourceKey;
    private String mimeType;
    private FileMeta fileMeta;
    private SourceType sourceType;
    private Long uploaderId;
    private Instant expiredAt;
    private Instant createdAt;
    private CloudResPresignedUrlResp presignedUrl;

    private final AuditContentFormat format = AuditContentFormat.IMAGE;
    public ImageAuditPayload(Resource res) {
        this.uuid = res.getUuid();
        this.resourceKey = res.getResourceKey();
        this.mimeType = res.getMimeType();
        this.fileMeta = JsonUtil.fromJson(res.getFileMeta(), FileMeta.class);
        this.sourceType = res.getSourceType();
        this.uploaderId = res.getUploaderId();
        this.expiredAt = res.getExpiredAt();
        this.createdAt = res.getCreatedAt();
    }

    @Override
    public String toJson() {
        return JsonUtil.toJson(this);
    }
}
