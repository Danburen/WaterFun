package org.waterwood.waterfunservicecore.entity.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "user_counter")
public class UserCounter {
    @Id
    @Column(name = "user_uid", nullable = false)
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_uid", nullable = false)
    private User user;

    @ColumnDefault("1")
    @Column(name = "level")
    private Byte level;

    @ColumnDefault("0")
    @Column(name = "exp")
    private Integer exp;

    @ColumnDefault("0")
    @Column(name = "follower_cnt")
    private Integer followerCnt;

    @ColumnDefault("0")
    @Column(name = "following_cnt")
    private Integer followingCnt;

    @ColumnDefault("0")
    @Column(name = "like_cnt")
    private Integer likeCnt;

    @ColumnDefault("0")
    @Column(name = "post_cnt")
    private Integer postCnt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ColumnDefault("'1'")
    @Column(name = "visible", columnDefinition = "tinyint UNSIGNED")
    private Short visible;

    public boolean isVisible() {
        return visible == 1;
    }
}