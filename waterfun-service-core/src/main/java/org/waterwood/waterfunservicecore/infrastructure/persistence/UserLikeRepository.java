package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.user.UserLike;
import org.waterwood.waterfunservicecore.entity.user.UserLikeId;

import java.util.List;

public interface UserLikeRepository extends JpaRepository<UserLike, UserLikeId> {

    @Query("SELECT ul.id.postId FROM UserLike ul WHERE ul.id.userId = :userId ORDER BY ul.createdAt DESC")
    Page<Long> findPostIdsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT ul.id.userId FROM UserLike ul WHERE ul.id.postId = :postId ORDER BY ul.createdAt DESC")
    List<Long> findUserIdsByPostId(@Param("postId") Long postId);

    @Query("SELECT ul.id.userId FROM UserLike ul WHERE ul.id.postId = :postId ORDER BY ul.createdAt DESC")
    List<Long> findUserIdsByPostId(@Param("postId") Long postId, Pageable pageable);

    long countByIdPostId(Long postId);

    long countByIdUserId(Long userId);
}