package org.waterwood.waterfunservicecore.entity.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "user_data")
@NoArgsConstructor
public class UserDatum {
    @Id
    @Column(name = "user_id", nullable = false)
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ColumnDefault("0")
    @Column(name = "email_verified")
    private Boolean emailVerified;

    @ColumnDefault("0")
    @Column(name = "phone_verified")
    private Boolean phoneVerified;

    @Column(name = "encryption_key_id", nullable = false, length = 50)
    private String encryptionKeyId;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "email_encrypted", length = 50)
    private String emailEncrypted;

    @Column(name = "email_hash", length = 65)
    private String emailHash;

    @Column(name = "phone_encrypted", length = 50)
    private String phoneEncrypted;

    @Column(name = "phone_hash", length = 65)
    private String phoneHash;

    public UserDatum(User user, String email, String phone, String encryptionKeyId) {
        this.user = user;
        this.id = user.getId();
        this.encryptionKeyId = encryptionKeyId;
        this.emailVerified = false;
        this.phoneVerified = false;
    }
}