package org.waterwood.waterfunservicecore.entity.ticket;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.security.PenaltyType;
import org.waterwood.waterfunservicecore.entity.user.User;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "ticket")
public class Ticket {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 64)
    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(name = "target_type", columnDefinition = "tinyint UNSIGNED not null")
    private TargetType targetType = TargetType.DEFAULT;

    @ColumnDefault("'1'")
    @Convert(disableConversion = true)
    @Enumerated
    @Column(name = "status", columnDefinition = "tinyint UNSIGNED not null")
    private TicketAuditStatus status = TicketAuditStatus.PENDING;

    @NotNull
    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitter", nullable = false)
    private User submitter;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "auditor")
    private User auditor;

    @Column(name = "audit_at")
    private Instant auditAt;

    @Enumerated
    @Column(name = "reject_type", columnDefinition = "tinyint UNSIGNED")
    private TicketRejectType rejectType = TicketRejectType.NONE;

    @Size(max = 255)
    @Column(name = "audit_note")
    private String auditNote;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Enumerated
    @Column(name = "ticket_type", columnDefinition = "tinyint UNSIGNED not null")
    private TicketType ticketType;

    @ColumnDefault("'0'")
    @Column(name = "penalty_type", columnDefinition = "tinyint UNSIGNED")
    private PenaltyType penaltyType = PenaltyType.UNSPECIFIED;

    @Size(max = 2000)
    @Column(name = "reply_content", length = 2000)
    private String replyContent;

    @Column(name = "target_user_uid")
    private Long targetUserUid;

}