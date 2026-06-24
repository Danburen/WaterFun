package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.waterwood.waterfunservicecore.entity.user.UserPenaltyHistory;

import java.util.Optional;

public interface UserPenaltyHistoryRepository extends JpaRepository<UserPenaltyHistory, Long> {
    Optional<UserPenaltyHistory> findTopByUserUidOrderByCreatedAtDesc(Long userUid);
}
