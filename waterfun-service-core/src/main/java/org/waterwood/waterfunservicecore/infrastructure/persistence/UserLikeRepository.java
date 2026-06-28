package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.user.UserLike;
import org.waterwood.waterfunservicecore.entity.user.UserLikeId;

public interface UserLikeRepository extends JpaRepository<UserLike, UserLikeId> {

    @Query("SELECT ul.id.postId FROM UserLike ul WHERE ul.id.userId = :userId ORDER BY ul.createdAt DESC")
    Page<Long> findPostIdsByUserId(@Param("userId") Long userId, Pageable pageable);

    long countByIdUserId(Long userId);
}