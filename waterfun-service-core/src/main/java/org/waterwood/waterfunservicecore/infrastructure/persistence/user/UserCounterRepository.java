package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.user.UserCounter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserCounterRepository extends JpaRepository<UserCounter, Long> {
    void deleteByUserUid(long attr0);

    Optional<UserCounter> findByUserUid(long attr0);

    @Modifying
    @Query("UPDATE UserCounter uc SET uc.likeCnt = GREATEST (uc.likeCnt - :count, 0) WHERE uc.user.uid = :userUid")
    void decreaseUserLikeCount(@Param("userUid") Long userUid, @Param("count") int count);

    @Modifying
    @Query("UPDATE UserCounter uc SET uc.likeCnt = uc.likeCnt + :count WHERE uc.user.uid = :userUid")
    void increaseUserLikeCount(@Param("userUid") Long userUid, @Param("count") int count);

    @Modifying
    @Query("UPDATE UserCounter uc SET uc.followerCnt = GREATEST (uc.followerCnt - :count, 0) WHERE uc.user.uid = :userUid")
    void decreaseUserFollowerCount(@Param("userUid") Long userUid, @Param("count") int count);
    @Modifying
    @Query("UPDATE UserCounter uc SET uc.followerCnt = uc.followerCnt + :count WHERE uc.user.uid = :userUid")
    void increaseUserFollowerCount(@Param("userUid") Long userUid, @Param("count") int count);

    @Modifying
    @Query("UPDATE UserCounter uc SET uc.followingCnt = GREATEST (uc.followingCnt - :count, 0) WHERE uc.user.uid = :userUid")
    void decreaseUserFollowingCount(@Param("userUid") Long userUid, @Param("count") int count);
    @Modifying
    @Query("UPDATE UserCounter uc SET uc.followingCnt = uc.followingCnt + :count WHERE uc.user.uid = :userUid")
    void increaseUserFollowingCount(@Param("userUid") Long userUid, @Param("count") int count);

    @Modifying
    @Query("UPDATE UserCounter uc SET uc.postCnt = GREATEST (uc.postCnt - :count, 0) WHERE uc.user.uid = :userUid")
    void decreaseUserPostCount(@Param("userUid") Long userUid, @Param("count") int count);
    @Modifying
    @Query("UPDATE UserCounter uc SET uc.postCnt = uc.postCnt + :count WHERE uc.user.uid = :userUid")
    void increaseUserPostCount(@Param("userUid") Long userUid, @Param("count") int count);

    @Modifying
    @Query("UPDATE UserCounter uc SET uc.collectCnt = GREATEST (uc.collectCnt - :count, 0) WHERE uc.user.uid = :userUid")
    void decreaseUserCollectionCount(@Param("userUid") Long userUid, @Param("count") int count);
    @Modifying
    @Query("UPDATE UserCounter uc SET uc.collectCnt = uc.collectCnt + :count WHERE uc.user.uid = :userUid")
    void increaseUserCollectionCount(@Param("userUid") Long userUid, @Param("count") int count);

    List<UserCounter> findAllByUserUidIn(List<Long> attr0);
}