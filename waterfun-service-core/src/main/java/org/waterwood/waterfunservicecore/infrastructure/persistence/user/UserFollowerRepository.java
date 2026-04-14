package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.user.UserFollower;

public interface UserFollowerRepository extends JpaRepository<UserFollower, Long> ,
        JpaSpecificationExecutor<UserFollower> {

    @Query("select distinct uf from UserFollower uf " +
            "join fetch uf.follower u " +
            "join fetch uf.counter  c " +
            "join fetch uf.profile  p " +
            "where uf.user.uid = :uid " +
            "order by uf.createdAt desc")
    Page<UserFollower> fetchFollowers(@Param("uid") Long uid, Pageable pageable);
    @Query("select distinct uf from UserFollower uf " +
            "join fetch uf.follower u " +
            "join fetch uf.counter  c " +
            "join fetch uf.profile  p " +
            "where uf.follower.uid = :uid " +
            "order by uf.createdAt desc")
    Page<UserFollower> fetchFollowings(@Param("uid") Long uid, Pageable pageable);

    void deleteByUserUid(Long userUid);
}