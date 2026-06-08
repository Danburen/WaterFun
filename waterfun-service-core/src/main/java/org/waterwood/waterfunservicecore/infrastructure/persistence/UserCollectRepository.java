package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.waterwood.waterfunservicecore.entity.user.UserCollect;
import org.waterwood.waterfunservicecore.entity.user.UserCollectId;

public interface UserCollectRepository extends JpaRepository<UserCollect, UserCollectId> {
}