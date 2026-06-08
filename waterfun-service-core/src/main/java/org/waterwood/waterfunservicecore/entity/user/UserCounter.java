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
    @MapsId
    @JoinColumn(name = "user_uid")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_uid", nullable = false)
    private User user;

    @ColumnDefault("0")
    @Column(name = "follower_cnt", columnDefinition = "int UNSIGNED")
    private Long followerCnt = 0L;

    @ColumnDefault("0")
    @Column(name = "following_cnt", columnDefinition = "int UNSIGNED")
    private Long followingCnt = 0L;

    @ColumnDefault("0")
    @Column(name = "like_cnt", columnDefinition = "int UNSIGNED")
    private Long likeCnt = 0L;

    @ColumnDefault("0")
    @Column(name = "post_cnt", columnDefinition = "int UNSIGNED")
    private Long postCnt = 0L;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @ColumnDefault("'0'")
    @Column(name = "collect_cnt", columnDefinition = "int UNSIGNED")
    private Long collectCnt = 0L;

}