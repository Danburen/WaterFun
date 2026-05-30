package org.waterwood.waterfunservicecore.api.moderation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.utils.JsonUtil;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostAuditPayload implements AuditPayload{
    private String title;
    private String subTitle;
    private String content;
    private String summary;

    private String coverageResUuid;
    private Long categoryId;
    private List<Long> tagIds;
    private List<String> newTagNames;

    public String toJson() {
        return JsonUtil.toJson(this);
    }

    public static PostAuditPayload fromJson(String json) {
        return JsonUtil.fromJson(json, PostAuditPayload.class);
    }
}
