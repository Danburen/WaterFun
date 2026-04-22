package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.waterwood.waterfunservicecore.entity.user.UserPreference;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
}