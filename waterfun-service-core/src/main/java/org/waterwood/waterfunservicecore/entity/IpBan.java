package org.waterwood.waterfunservicecore.entity;

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
@Table(name = "ip_ban")
public class IpBan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 45)
    @NotNull
    @Column(name = "ip", nullable = false, length = 45)
    private String ip;

    @Size(max = 100)
    @NotNull
    @ColumnDefault("''")
    @Column(name = "reason", nullable = false, length = 100)
    private String reason = "";

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "banned_at", nullable = false)
    private Instant bannedAt = Instant.now();

    @NotNull
    @Column(name = "expires_at")
    private Instant expiresAt;

}