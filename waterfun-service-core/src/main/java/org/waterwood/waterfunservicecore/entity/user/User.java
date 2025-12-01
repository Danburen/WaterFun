package org.waterwood.waterfunservicecore.entity.user;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import java.time.Instant;
@Data
@Entity
@NoArgsConstructor
@Table(name = "user", schema = "waterfun")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uid", nullable = false, length = 16)
    private String uid;

    @Column(name = "username", nullable = false, length = 32)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ColumnDefault("'ACTIVE'")
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status")
    private AccountStatus accountStatus;

    @Column(name = "status_changed_at")
    private Instant statusChangedAt;

    @Column(name = "status_change_reason")
    private String statusChangeReason;



    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;
    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.accountStatus = AccountStatus.ACTIVE;
    }
}


