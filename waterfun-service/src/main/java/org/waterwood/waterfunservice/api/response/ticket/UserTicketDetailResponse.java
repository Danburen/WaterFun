package org.waterwood.waterfunservice.api.response.ticket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.ticket.TicketAuditStatus;
import org.waterwood.waterfunservicecore.entity.ticket.TicketType;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserTicketDetailResponse {
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

    private List<EvidenceItem> evidence;
    private Timeline timeline;
    private List<ReplyItem> replies;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenceItem {
        private String uuid;
        private String url;
        private Instant expireAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Timeline {
        private Instant submittedAt;
        private Instant reviewedAt;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReplyItem {
        private Long id;
        private String content;
        private String senderName;
        private Instant createdAt;
    }
}
