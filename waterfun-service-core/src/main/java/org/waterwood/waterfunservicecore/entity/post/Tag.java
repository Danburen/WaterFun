package org.waterwood.waterfunservicecore.entity.post;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.waterwood.waterfunservicecore.entity.user.User;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "tag")
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "bigint UNSIGNED not null")
    private Long id;

    @Size(max = 30)
    @NotNull
    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Size(max = 50)
    @NotNull
    @Column(name = "slug", nullable = false, length = 50)
    private String slug;

    @Column(name = "description")
    private String description;

    @ColumnDefault("'0'")
    @Column(name = "usage_count", columnDefinition = "int UNSIGNED")
    private Integer usageCount = 0;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @CreationTimestamp
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "update_at", nullable = false)
    private Instant updateAt = Instant.now();

    @ColumnDefault("0")
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

}