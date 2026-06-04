package org.waterwood.waterfunservicecore.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.waterwood.waterfunservicecore.entity.resource.Resource;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "banner")
public class Banner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 64)
    @NotNull
    @Column(name = "title", nullable = false, length = 64)
    private String title;

    @Size(max = 128)
    @Column(name = "subtitle", length = 128)
    private String subtitle;

    @Size(max = 255)
    @Column(name = "link_url")
    private String linkUrl;

    @ColumnDefault("'1'")
    @Column(name = "position", columnDefinition = "tinyint UNSIGNED not null")
    private BannerPosition position = BannerPosition.HOME;

    @ColumnDefault("'0'")
    @Column(name = "sort_no", columnDefinition = "int not null")
    private Integer sortNo = 0;

    @ColumnDefault("'1'")
    @Column(name = "status", columnDefinition = "tinyint UNSIGNED not null")
    private VisibleStatus status = VisibleStatus.SHOW;

    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "resource_uuid", referencedColumnName = "uuid")
    private Resource resource;

}