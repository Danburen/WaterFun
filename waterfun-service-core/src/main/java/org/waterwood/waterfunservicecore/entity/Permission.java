package org.waterwood.waterfunservicecore.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.*;
import org.waterwood.api.VO.ToOptionVO;
import org.waterwood.api.enums.PermissionType;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "permission")
@NoArgsConstructor
public class Permission implements ToOptionVO<Integer> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description")
    private String description;

    @ColumnDefault("'API'")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PermissionType type;

    @Column(name = "resource")
    private String resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "parent_id")
    private Permission parent;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "update_at")
    @UpdateTimestamp
    private Instant updateAt;

    @ColumnDefault("1")
    @Column(name = "order_weight")
    private Integer orderWeight = 1;

    @ColumnDefault("'0'")
    @Column(name = "is_system", columnDefinition = "tinyint UNSIGNED")
    private Boolean isSystem = false;

}