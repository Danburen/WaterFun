package org.waterwood.waterfunservicecore.entity.resource;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.waterwood.common.io.ResourceType;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "resource")
public class Resource {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "resource_key", nullable = false)
    private String resourceKey;

    @Column(name = "resource_type", columnDefinition = "tinyint UNSIGNED not null")
    private ResourceType resourceType;

    @Size(max = 128)
    @Column(name = "mime_type", length = 128)
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @ColumnDefault("'0")
    @Column(name = "source_type", columnDefinition = "tinyint UNSIGNED not null")
    private SourceType sourceType = SourceType.SYSTEM;

    @Column(name = "uploader_id")
    private Long uploaderId = 0L;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Size(max = 36)
    @NotNull
    @Column(name = "uuid", nullable = false, length = 36)
    private String uuid;

    @ColumnDefault("'0'")
    @Column(name = "status", columnDefinition = "tinyint UNSIGNED not null")
    private ResourceStatus status = ResourceStatus.UPLOAD_PENDING;

    @Column(name = "file_meta", columnDefinition = "json")
//    @JdbcTypeCode(SqlTypes.JSON)
    private String fileMeta;

}