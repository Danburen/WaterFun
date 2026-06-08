package org.waterwood.waterfunservicecore.entity.post;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
public class PostLikeId implements Serializable {
    private static final long serialVersionUID = -1786720103286371088L;
    @NotNull
    @Column(name = "post_id", nullable = false)
    private Long postId;

    @NotNull
    @Column(name = "user_uid", nullable = false)
    private Long userUid;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        PostLikeId entity = (PostLikeId) o;
        return Objects.equals(this.postId, entity.postId) &&
                Objects.equals(this.userUid, entity.userUid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId, userUid);
    }

}