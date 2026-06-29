package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.user.UserFollower;
import org.waterwood.waterfunservicecore.entity.user.UserFollowerId;

public interface UserFollowerRepository extends JpaRepository<UserFollower, UserFollowerId> ,
        JpaSpecificationExecutor<UserFollower> {

    @Query("SELECT uf.id.followerUid FROM UserFollower uf " +
            "WHERE uf.id.userUid = :uid " +
            "ORDER BY uf.createdAt DESC ")
    Page<Long> findByUserUid(@Param("uid") Long uid, Pageable pageable);
    @Query("SELECT uf.id.userUid FROM UserFollower uf " +
            "WHERE uf.id.followerUid = :uid " +
            "ORDER BY uf.createdAt DESC ")
    Page<Long> findByFollowerUid(@Param("uid") long uid, Pageable pageable);

}