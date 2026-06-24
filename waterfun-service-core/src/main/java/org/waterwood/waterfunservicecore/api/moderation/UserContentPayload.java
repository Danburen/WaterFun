package org.waterwood.waterfunservicecore.api.moderation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.utils.JsonUtil;
import org.waterwood.waterfunservicecore.entity.audit.AuditContentFormat;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserContentPayload implements AuditPayload {
    private String content;
    private String contact;

    private final AuditContentFormat format = AuditContentFormat.TXT;

    @Override
    public String toJson() {
        return JsonUtil.toJson(this);
    }
}
