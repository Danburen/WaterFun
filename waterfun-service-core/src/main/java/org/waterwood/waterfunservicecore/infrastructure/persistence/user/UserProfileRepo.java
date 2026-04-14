package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;

import java.util.Optional;

public interface UserProfileRepo extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findUserProfileByUserUid(long userUid);

    void deleteByUserUid(long attr0);
}
