package org.waterwood.waterfunadminservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunadminservice.api.response.content.audit.ModerationTaskPayloadRes;
import org.waterwood.waterfunservicecore.entity.audit.AuditContentFormat;
import org.waterwood.waterfunservicecore.entity.audit.task.TargetType;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModerateTaskResponse {
    private Long id;
    private TargetType targetType;
    private String targetId;
    private ModerationTaskPayloadRes payload;
    private Long submitterId;
    private Instant submitAt;
}
