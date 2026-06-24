package org.waterwood.waterfunservice.api.response.ticket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.ticket.TicketAuditStatus;
import org.waterwood.waterfunservicecore.entity.ticket.TicketType;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserTicketListResponse {
    private Long ticketId;
    private TicketType ticketType;
    private TicketAuditStatus status;
    private String content;
    private String targetId;
    private TargetType targetType;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant auditAt;
    private String auditNote;
    private String rejectType;
    private int evidenceCount;
}
