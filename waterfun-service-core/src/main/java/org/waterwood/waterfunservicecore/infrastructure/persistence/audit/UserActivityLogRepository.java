package org.waterwood.waterfunservicecore.infrastructure.persistence.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.audit.UserActivityLog;

import java.util.List;

public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {

    List<UserActivityLog> findTop30ByOrderByCreatedAtDesc();

    @Query("SELECT ual FROM UserActivityLog ual WHERE ual.userId IN :userIds ORDER BY ual.createdAt DESC")
    List<UserActivityLog> findLatestByUserIds(@Param("userIds") List<Long> userIds, Pageable pageable);

    @Query("SELECT ual FROM UserActivityLog ual WHERE ual.userId = :userId ORDER BY ual.createdAt DESC")
    List<UserActivityLog> findLatestByUserId(@Param("userId") Long userId, Pageable pageable);
}
