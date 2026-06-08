package org.waterwood.waterfunservicecore.infrastructure.persistence.notification;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.notification.BusinessType;
import org.waterwood.waterfunservicecore.entity.notification.Inbox;
import org.waterwood.waterfunservicecore.entity.notification.NoticeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InboxRepository extends JpaRepository<Inbox, Long> {
    @Query("""
        SELECT i FROM Inbox i
        WHERE i.user.uid = :userId
          AND i.isDeleted = false
          AND (:cursor IS NULL OR i.id < :cursor)
          AND (:unreadOnly IS NULL OR i.isRead = false)
        ORDER BY i.id DESC
        LIMIT :limit
        """)
    List<Inbox> findByCursor(
            @Param("userId") Long userId,
            @Param("cursor") Long cursor,
            @Param("unreadOnly") Boolean unreadOnly,
            @Param("limit") int limit
    );

    Integer countByIsRead(Boolean isRead);

    Integer countByIsReadAndUserUid(Boolean isRead, Long userUid);

    @Transactional
    @Modifying
    @Query("UPDATE Inbox i SET i.isRead = true WHERE i.user.uid = :userUid AND i.isRead = false")
    void markAllReadByUserUid(@Param("userUid") Long userUid,@Param("readAt") Instant readAt);

    @Transactional
    @Modifying
    @Query("""
    UPDATE Inbox i
    SET i.isRead = true
    WHERE i.id IN :ids
      AND i.user.uid = :userId
      AND i.isRead = false
    """)
    int markReadBatch(@Param("ids") List<Long> ids, @Param("userId") Long userId,@Param("readAt") Instant readAt);

    List<Inbox> findByUserUidAndNoticeTypeAndBusinessTypeAndTargetId(Long userUid, NoticeType noticeType, BusinessType businessType, String targetId);

    List<Inbox> findByUserUidAndNoticeTypeAndBusinessTypeAndTargetIdOrderByCreatedAtDesc(Long userUid, NoticeType noticeType, BusinessType businessType, @Size(max = 64) String targetId);

    Integer countByIsReadAndUserUidAndIsDeleted(Boolean isRead, Long userUid, Boolean isDeleted);
}
