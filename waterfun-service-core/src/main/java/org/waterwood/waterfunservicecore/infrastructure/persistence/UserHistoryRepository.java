package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.waterwood.waterfunservicecore.entity.user.UserHistory;
import org.waterwood.waterfunservicecore.entity.user.UserHistoryId;

public interface UserHistoryRepository extends JpaRepository<UserHistory, UserHistoryId> {
}