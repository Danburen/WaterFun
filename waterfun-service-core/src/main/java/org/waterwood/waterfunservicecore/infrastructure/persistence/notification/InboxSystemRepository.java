package org.waterwood.waterfunservicecore.infrastructure.persistence.notification;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.api.CursorPage;
import org.waterwood.waterfunservicecore.entity.notification.InboxSystem;

import java.util.List;

public interface InboxSystemRepository extends JpaRepository<InboxSystem, Long> {
    @Query("""
        SELECT i FROM InboxSystem i
        WHERE i.user.uid = :userId
          AND i.isDeleted = false
          AND (:cursor IS NULL OR i.id < :cursor)
          AND (:unreadOnly IS NULL OR i.isRead = false)
        ORDER BY i.id DESC
        LIMIT :limit
        """)
    List<InboxSystem> findByCursor(
            @Param("userId") Long userId,
            @Param("cursor") Long cursor,
            @Param("unreadOnly") Boolean unreadOnly,
            @Param("limit") int limit
    );

    Integer countByIsRead(Boolean isRead);

    Integer countByIsReadAndUserUid(Boolean isRead, Long userUid);

    @Transactional
    @Modifying
    @Query("UPDATE InboxSystem i SET i.isRead = true WHERE i.user.uid = :userUid AND i.isRead = false")
    void markAllReadByUserUid(@Param("userUid") Long userUid);

    @Modifying
    @Query("""
    UPDATE InboxSystem i
    SET i.isRead = true, i.readAt = CURRENT_TIMESTAMP
    WHERE i.id IN :ids
      AND i.user = :userId
      AND i.isRead = false
    """)
    int markReadBatch(@Param("ids") List<Long> ids, @Param("userId") Long userId);
}
