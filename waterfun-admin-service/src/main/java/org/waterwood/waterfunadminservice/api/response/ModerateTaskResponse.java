package org.waterwood.waterfunadminservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunadminservice.api.response.content.audit.ModerationTaskPayloadRes;
import org.waterwood.waterfunservicecore.entity.audit.AuditContentFormat;
import org.waterwood.waterfunservicecore.entity.audit.task.MediaResourceType;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModerateTaskResponse {
    private Long id;
    private MediaResourceType targetType;
    private String targetId;
    private String content;
    private AuditContentFormat contentFormat;
    private ModerationTaskPayloadRes payload;
    private Long submitterId;
    private Instant submitAt;
}
