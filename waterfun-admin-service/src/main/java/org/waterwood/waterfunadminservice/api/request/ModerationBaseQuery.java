package org.waterwood.waterfunadminservice.api.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.RequestParam;
import org.waterwood.waterfunservicecore.entity.Priority;
import org.waterwood.waterfunservicecore.entity.audit.AuditStatus;
import org.waterwood.waterfunservicecore.entity.audit.AuditTriggerType;

import java.time.Instant;

public record ModerationBaseQuery(AuditTriggerType triggerType,
                                  Priority priority,
                                  AuditStatus status,
                                  Long submitterUid,
                                  Instant submitAtStart,
                                  Instant submitAtEnd) {
}
