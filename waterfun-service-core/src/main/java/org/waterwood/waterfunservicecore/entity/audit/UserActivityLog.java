package org.waterwood.waterfunservicecore.entity.audit;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.waterwood.waterfunservicecore.entity.notification.BusinessType;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "user_activity_log")
public class UserActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated
    @Column(name = "action_type", columnDefinition = "tinyint UNSIGNED not null")
    private UserActionType actionType = UserActionType.UNKNOWN;

    @Column(name = "target_id")
    private Long targetId;

    @Size(max = 45)
    @NotNull
    @Column(name = "ip", nullable = false, length = 45)
    private String ip;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "business_type", columnDefinition = "tinyint UNSIGNED not null")
    private BusinessType businessType = BusinessType.NONE;

}