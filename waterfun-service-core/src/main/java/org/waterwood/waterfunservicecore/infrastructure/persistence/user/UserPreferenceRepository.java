package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.waterwood.waterfunservicecore.entity.user.UserPreference;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    @Query("SELECT up.locale FROM UserPreference up WHERE up.user.uid = :userUid")
    String getLocaleByUserUid(Long userUid);
}