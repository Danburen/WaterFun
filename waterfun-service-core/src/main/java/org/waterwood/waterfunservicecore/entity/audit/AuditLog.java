package org.waterwood.waterfunservicecore.entity.audit;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;


    @Column(name = "user_id")
    private Long userId;

    @Size(max = 50)
    @Column(name = "username", length = 50)
    private String username;

    @ColumnDefault("'0'")
    @Column(name = "action", columnDefinition = "tinyint UNSIGNED not null")
    private AuditLogActionType action = AuditLogActionType.UNKNOWN;

    @Size(max = 45)
    @NotNull
    @Column(name = "ip", nullable = false, length = 45)
    private String ip;

    @Column(name = "device_info")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> deviceInfo;

    @Size(max = 50)
    @Column(name = "country", length = 50)
    private String country;

    @Size(max = 50)
    @Column(name = "province", length = 50)
    private String province;

    @Size(max = 50)
    @Column(name = "city", length = 50)
    private String city;

    @ColumnDefault("'1'")
    @Column(name = "status", columnDefinition = "tinyint UNSIGNED not null")
    private AuditLogStatusType status = AuditLogStatusType.SUCCESS;

    @Size(max = 64)
    @Column(name = "fail_reason", length = 64)
    private String failReason;

    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

}