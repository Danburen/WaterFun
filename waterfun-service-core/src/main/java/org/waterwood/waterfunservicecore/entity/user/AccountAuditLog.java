package org.waterwood.waterfunservicecore.entity.user;

import jakarta.persistence.*;
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
@Table(name = "account_audit_log")
public class AccountAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_uid", nullable = false)
    private Long userUid;

    @Lob
    @Column(name = "action", nullable = false)
    private String action;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "action_time")
    private Instant actionTime;

    @Column(name = "performed_by", length = 50)
    private String performedBy;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "device_info")
    private String deviceInfo;

    @Column(name = "old_value")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> oldValue;

    @Column(name = "new_value")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> newValue;

}