package org.waterwood.waterfunservicecore.infrastructure.security;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "encryption_data_key")
public class EncryptionDataKey {
    @Id
    @Column(name = "id", nullable = false, length = 50)
    private String id;

    @Column(name = "encrypted_key", nullable = false, length = 512)
    private String encryptedKey;

    @ColumnDefault("'AES'")
    @Column(name = "algorithm", nullable = false, length = 20)
    private String algorithm;

    @ColumnDefault("256")
    @Column(name = "key_length", nullable = false)
    private Integer keyLength;

    @Enumerated(EnumType.STRING)
    @Column(name = "key_status", nullable = false, length = 20)
    @ColumnDefault("'PENDING_ACTIVATION'")
    private KeyStatus keyStatus;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "description")
    private String description;

}