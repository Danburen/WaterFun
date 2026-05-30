package org.waterwood.waterfunservicecore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
public class PostResourceId implements Serializable {
    private static final long serialVersionUID = -5824495977730836706L;
    @NotNull
    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Size(max = 36)
    @NotNull
    @Column(name = "resource_uuid", nullable = false, length = 36)
    private String resourceUuid;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        PostResourceId entity = (PostResourceId) o;
        return Objects.equals(this.postId, entity.postId) &&
                Objects.equals(this.resourceUuid, entity.resourceUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId, resourceUuid);
    }

}