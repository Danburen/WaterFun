package org.waterwood.waterfunservicecore.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.*;
import org.waterwood.api.VO.ToOptionVO;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "role")
@NoArgsConstructor
public class Role implements ToOptionVO<Integer> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "parent_id")
    private Role parent;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "update_at")
    @UpdateTimestamp
    private Instant updateAt;

    @Size(max = 50)
    @NotNull
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @ColumnDefault("1")
    @Column(name = "order_weight")
    private Integer orderWeight = 0;

    @ColumnDefault("'0'")
    @Column(name = "is_system", columnDefinition = "tinyint UNSIGNED")
    private Boolean isSystem = false;

    public Role(String name, String description) {
        this.name = name;
        this.description = description;
        this.parent = null;
        this.createdAt = Instant.now();
    }
}