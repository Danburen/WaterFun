package org.waterwood.waterfunservicecore.entity.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.waterwood.waterfunservicecore.entity.audit.AuditType;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.security.PenaltyType;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "user_penalty_history")
public class UserPenaltyHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Convert(disableConversion = true)
    @Enumerated
    @Column(name = "penalty_type", columnDefinition = "tinyint UNSIGNED not null")
    private PenaltyType penaltyType = PenaltyType.UNSPECIFIED;

    @Size(max = 64)
    @Column(name = "target_id", length = 64)
    private String targetId;

    @Convert(disableConversion = true)
    @Enumerated
    @Column(name = "target_type", columnDefinition = "tinyint UNSIGNED not null")
    private TargetType targetType = TargetType.DEFAULT;

    @Convert(disableConversion = true)
    @Enumerated
    @Column(name = "penalty_reason_type", columnDefinition = "tinyint UNSIGNED")
    private AuditType penaltyReasonType = AuditType.OTHER;

    @Size(max = 255)
    @Column(name = "reason")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "operator_id")
    private User operator;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

}