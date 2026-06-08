package org.waterwood.waterfunservicecore.entity.user;

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
public class UserHistoryId implements Serializable {
    private static final long serialVersionUID = -7875412065430352945L;
    @NotNull
    @Column(name = "user_uid", nullable = false)
    private Long userId;

    @NotNull
    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        UserHistoryId entity = (UserHistoryId) o;
        return Objects.equals(this.postId, entity.postId) &&
                Objects.equals(this.userId, entity.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId, userId);
    }

}