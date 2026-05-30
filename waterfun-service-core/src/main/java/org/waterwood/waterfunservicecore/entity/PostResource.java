package org.waterwood.waterfunservicecore.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.waterwood.waterfunservicecore.entity.post.Post;
import org.waterwood.waterfunservicecore.entity.resource.Resource;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "post_resource")
public class PostResource {
    @EmbeddedId
    private PostResourceId id;

    @MapsId("postId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @MapsId("resourceUuid")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_uuid", nullable = false, referencedColumnName = "uuid")
    private Resource resourceUuid;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

}