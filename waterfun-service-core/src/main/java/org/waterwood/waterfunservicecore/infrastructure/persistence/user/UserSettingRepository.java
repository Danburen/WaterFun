package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.waterwood.waterfunservicecore.entity.user.ProfileVisibility;
import org.waterwood.waterfunservicecore.entity.user.UserSetting;

import java.util.List;

public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
    List<UserSetting> findByWorkVisibilityNot(ProfileVisibility visibility);
}
