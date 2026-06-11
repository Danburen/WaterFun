package org.waterwood.waterfunservicecore.api.moderation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.utils.JsonUtil;
import org.waterwood.waterfunservicecore.entity.audit.AuditContentFormat;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReplyPayload implements AuditPayload {
    private String content;

    private Long postId;
    private Long commentId;
    private Long replierUid;

    @Override
    public String toJson() {
        return JsonUtil.toJson(this);
    }

    @Override
    public AuditContentFormat getFormat() {
        return AuditContentFormat.TXT;
    }
}
