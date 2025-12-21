package org.waterwood.waterfunservicecore.entity.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name = "user_profile", schema = "waterfun")
public class UserProfile {
    @NotNull
    @Id
    @Column(name = "user_uid", nullable = false)
    private Long uid;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_uid", nullable = false)
    @ToString.Exclude
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

    @Column(name = "birthday")
    private LocalDate birthday;

    @Size(max = 50)
    @Column(name = "residence", length = 50)
    private String residence;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "update_at")
    private Instant updateAt;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        UserProfile that = (UserProfile) o;
        return getUid() != null && Objects.equals(getUid(), that.getUid());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}