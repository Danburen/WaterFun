package org.waterwood.waterfunservicecore.entity.security;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "ip_access_log", indexes = {
        @Index(name = "idx_ip", columnList = "ip"),
        @Index(name = "idx_country", columnList = "country"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_ip_created", columnList = "ip, created_at")
})
public class IpAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 45)
    @NotNull
    @Column(name = "ip", nullable = false, length = 45)
    private String ip;

    @Size(max = 255)
    @NotNull
    @Column(name = "request_path", nullable = false, length = 255)
    private String requestPath;

    @Size(max = 10)
    @NotNull
    @Column(name = "request_method", nullable = false, length = 10)
    private String requestMethod;

    @Column(name = "user_uid")
    private Long userUid;

    @Column(name = "http_status")
    private Short httpStatus;

    @Size(max = 64)
    @Column(name = "country")
    private String country;

    @Size(max = 64)
    @Column(name = "province")
    private String province;

    @Size(max = 64)
    @Column(name = "city")
    private String city;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
