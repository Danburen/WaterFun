package org.waterwood.waterfunservice.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serial;
import java.util.Objects;

@Getter
@Setter
@Embeddable
public class UserLevelMappingId implements java.io.Serializable {
    @Serial
    private static final long serialVersionUID = -4882739345503385898L;
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "level_id", nullable = false)
    private Integer levelId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        UserLevelMappingId entity = (UserLevelMappingId) o;
        return Objects.equals(this.levelId, entity.levelId) &&
                Objects.equals(this.userId, entity.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(levelId, userId);
    }

}