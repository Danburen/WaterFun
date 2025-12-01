package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;

import java.util.Optional;

public interface UserProfileRepo extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findUserProfileByUserId(long userId);
    @Modifying
    @Query("UPDATE UserProfile u SET u.avatarUrl = :avatarUrl WHERE u.user = :userId")
    int updateAvatarUrl(@Param("userId") Long userId, @Param("avatarUrl") String avatarUrl);
}
