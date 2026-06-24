package org.waterwood.waterfunservicecore.api.moderation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.utils.JsonUtil;
import org.waterwood.waterfunservicecore.entity.audit.AuditContentFormat;
import org.waterwood.waterfunservicecore.entity.audit.AuditType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportPayload implements AuditPayload {
    private AuditType type;
    private String reason;
    private Long reporterUid;

    private final AuditContentFormat format = AuditContentFormat.TXT;

    @Override
    public String toJson() {
        return JsonUtil.toJson(this);
    }
}
