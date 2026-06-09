package org.waterwood.waterfunadminservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogActionType;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogStatusType;

import java.time.Instant;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogResponse {
    private Long id;
    private Long userId;
    private String username;
    private AuditLogActionType action;
    private String ip;
    private Map<String, Object> deviceInfo;
    private String country;
    private String province;
    private String city;
    private AuditLogStatusType status;
    private String failReason;
    private Instant createdAt;
}
