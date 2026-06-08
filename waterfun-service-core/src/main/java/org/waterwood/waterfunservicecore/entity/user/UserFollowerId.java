package org.waterwood.waterfunservicecore.entity.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
public class UserFollowerId implements Serializable {
    private static final long serialVersionUID = -147596655352207229L;
    @NotNull
    @Column(name = "user_uid", nullable = false)
    private Long userUid;

    @NotNull
    @Column(name = "follower_uid", nullable = false)
    private Long followerUid;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        UserFollowerId entity = (UserFollowerId) o;
        return Objects.equals(this.userUid, entity.userUid) &&
                Objects.equals(this.followerUid, entity.followerUid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userUid, followerUid);
    }

}