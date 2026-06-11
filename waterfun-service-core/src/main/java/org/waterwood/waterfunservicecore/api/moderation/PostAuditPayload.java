package org.waterwood.waterfunservicecore.api.moderation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.utils.JsonUtil;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.audit.AuditContentFormat;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostAuditPayload implements AuditPayload{
    private String title;
    private String subTitle;
    private String content;
    private String summary;
    private Long authorUid;

    private String coverageResUuid;
    private Long categoryId;
    private List<Long> tagIds;
    private List<String> newTagNames;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private CloudResPresignedUrlResp coverResPresignedUrl;

    @Override
    public String toJson() {
        return JsonUtil.toJson(this);
    }

    @Override
    public AuditContentFormat getFormat() {
        return AuditContentFormat.RICH;
    }
}
