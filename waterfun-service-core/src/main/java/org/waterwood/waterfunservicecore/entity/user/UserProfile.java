package org.waterwood.waterfunservicecore.entity.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Data
@Entity
@Table(name = "user_profile", schema = "waterfun")
public class UserProfile {
    @NotNull
    @Id
    @Column(name = "user_id", nullable = false)
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    @Size(max = 12)
    @Column(name = "nickname", length = 12)
    private String nickname;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Lob
    @Column(name = "bio")
    private String bio;

    @ColumnDefault("'UNKNOWN'")
    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

}