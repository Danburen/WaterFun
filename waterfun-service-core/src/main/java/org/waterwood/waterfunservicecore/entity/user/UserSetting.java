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
@Table(name = "user_setting")
public class UserSetting {
    @Id
    @Column(name = "user_uid", nullable = false)
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_uid", nullable = false)
    private User user;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "profile_visibility", nullable = false)
    private ProfileVisibility profileVisibility = ProfileVisibility.PUBLIC;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "work_visibility", nullable = false)
    private ProfileVisibility workVisibility = ProfileVisibility.PUBLIC;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "comment_permission", nullable = false)
    private ContentPermission commentPermission = ContentPermission.ALL;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "message_permission", nullable = false)
    private ContentPermission messagePermission = ContentPermission.ALL;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "allow_follow", nullable = false)
    private Boolean allowFollow = true;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "show_active_status", nullable = false)
    private Boolean showActiveStatus = true;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "message_notifications", nullable = false)
    private Boolean messageNotifications = true;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "comment_notifications", nullable = false)
    private Boolean commentNotifications = true;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "like_notifications", nullable = false)
    private Boolean likeNotifications = true;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "follow_notifications", nullable = false)
    private Boolean followNotifications = true;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "event_notifications", nullable = false)
    private Boolean eventNotifications = true;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "email_notifications", nullable = false)
    private Boolean emailNotifications = false;

    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

}
