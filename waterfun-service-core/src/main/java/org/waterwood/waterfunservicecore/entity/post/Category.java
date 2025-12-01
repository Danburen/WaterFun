package org.waterwood.waterfunservicecore.entity.post;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
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
@Table(name = "category")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "int UNSIGNED not null")
    private Integer id;

    @Size(max = 50)
    @NotEmpty
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Size(max = 50)
    @NotEmpty
    @Column(name = "slug", nullable = false, length = 50)
    private String slug;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "parent_id", columnDefinition = "int UNSIGNED")
    private Integer parentId;

    @ColumnDefault("0")
    @Column(name = "sort_order")
    private Integer sortOrder;

    @ColumnDefault("1")
    @Column(name = "is_active")
    private Boolean isActive;

    @UpdateTimestamp
    @Column(name = "update_at", nullable = false)
    private Instant updateAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;
}