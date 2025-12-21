package org.waterwood.waterfunservicecore.entity.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "user_follow")
public class UserFollower {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_uid", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "follower_uid", nullable = false)
    private User follower;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_uid", referencedColumnName = "user_uid",
            insertable = false, updatable = false)
    private UserCounter counter;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_uid", referencedColumnName = "user_uid",
            insertable = false, updatable = false)
    private UserProfile profile;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

}