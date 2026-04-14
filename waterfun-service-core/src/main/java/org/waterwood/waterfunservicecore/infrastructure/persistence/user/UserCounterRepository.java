package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.waterwood.waterfunservicecore.entity.user.UserCounter;

public interface UserCounterRepository extends JpaRepository<UserCounter, Long> {
    void deleteByUserUid(long attr0);
}