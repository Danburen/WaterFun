package org.waterwood.waterfunadminservice.api.request.ticket;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.waterwood.waterfunservicecore.entity.security.BanReasonType;
import org.waterwood.waterfunservicecore.entity.security.PenaltyType;
import org.waterwood.waterfunservicecore.entity.ticket.TicketRejectType;

@Data
public class TicketReviewRequest {
    @NotNull
    private ReviewAction action;

    private TicketRejectType rejectType;
    private String auditNote;

    private PenaltyType penaltyType;
    private Long penaltyDurationHours;
    private BanReasonType banReasonType;

    /** 可选的回复内容。填写则替换默认消息文本，不填则根据 ticketType + status 生成默认消息。 */
    private String replyContent;

    public enum ReviewAction {
        APPROVE, REJECT
    }
}
