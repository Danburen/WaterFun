package org.waterwood.waterfunservicecore.api.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.security.PenaltyType;
import org.waterwood.waterfunservicecore.entity.ticket.TicketAuditStatus;
import org.waterwood.waterfunservicecore.entity.ticket.TicketRejectType;
import org.waterwood.waterfunservicecore.entity.ticket.TicketType;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketMessage implements Serializable {
    private Long ticketId;
    private TicketType ticketType;
    private String targetId;
    private TargetType targetType;
    private TicketAuditStatus status;
    private Long submitterId;
    private Instant auditAt;
    private String rejectReason;
    private TicketRejectType rejectType;

    /** 处罚类型（仅 CONTENT_REPORT 批准时设置）。 */
    private PenaltyType penaltyType;

    /** 被处罚/被举报用户的 UID（仅 CONTENT_REPORT 批准时设置）。 */
    private Long targetUserUid;

    /** 被处罚/被举报用户的显示名称。 */
    private String targetUserDisplayName;

    /** 管理员自定义回复内容，为空时使用默认消息模板。 */
    private String replyContent;

    @Builder.Default
    private Instant sendTime = Instant.now();
}
