package org.waterwood.waterfunadminservice.api.response.ticket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunadminservice.api.response.BanStatusResponse;
import org.waterwood.waterfunadminservice.api.response.user.UserAdminBrief;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.ticket.TicketAuditStatus;
import org.waterwood.waterfunservicecore.entity.ticket.TicketRejectType;
import org.waterwood.waterfunservicecore.entity.ticket.TicketType;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private Long ticketId;
    private TicketType ticketType;
    private String targetId;
    private TargetType targetType;
    private TicketAuditStatus status;
    private String content;
    private String rejectType;
    private String auditNote;
    private String replyContent;
    private UserAdminBrief submitter;
    private UserAdminBrief targetUser;
    private UserBrief auditor;
    private Instant createdAt;
    private Instant auditAt;
    private Instant updatedAt;

    private List<String> evidenceResourceUuids;
    private PenaltyDetail originalPenalty;
    private BanStatusResponse currentBans;
    private Timeline timeline;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PenaltyDetail {
        private String penaltyType;
        private String reason;
        private String operatorName;
        private Instant appliedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Timeline {
        private Instant submittedAt;
        private Instant reviewedAt;
        private String status;
    }
}
