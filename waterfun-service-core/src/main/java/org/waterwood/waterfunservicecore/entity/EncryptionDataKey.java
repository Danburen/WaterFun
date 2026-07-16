package org.waterwood.waterfunservicecore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.waterwood.waterfunservicecore.infrastructure.security.Algorithm;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "encryption_data_key")
public class EncryptionDataKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;
    @Column(name = "key_id", nullable = false, length = 50)
    private String keyId;
    @Column(name = "encrypted_key", nullable = false, length = 512)
    private String encryptedKey;

    @ColumnDefault("'0'")
    @Column(name = "algorithm", columnDefinition = "tinyint UNSIGNED not null")
    private Algorithm algorithm = Algorithm.AES;

    @ColumnDefault("256")
    @Column(name = "key_length", nullable = false)
    private Short keyLength = 256;

    @ColumnDefault("'0'")
    @Column(name = "key_status", columnDefinition = "tinyint UNSIGNED not null")
    private KeyStatus keyStatus = KeyStatus.PENDING_ACTIVATION;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "description")
    private String description;

    @Column(name = "key_purpose", length = 30)
    private String keyPurpose;

}