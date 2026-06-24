package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserBriefDO;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> , JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);

    @Modifying
    @Query("UPDATE User u SET u.passwordHash = :passwordHash WHERE u.uid = :userUid")
    int updatePassword(@Param("userUid") Long userUid,@Param("passwordHash") String passwordHash);

    List<User> getUsersByUid(Long id);

    Optional<User> findUserByUid(Long uid);
    @Query("SELECT u FROM User u JOIN UserCounter c WHERE u.uid IN :userUids"
    )
    List<User> findAllVisibleUsersByIn(@Param("userUids") List<Long> userUids);

    boolean deleteUserByUid(Long uid);

    int deleteUserByUidIn(Collection<Long> uids);


    @Query("SELECT u.avatarResource.uuid FROM User u JOIN u.avatarResource WHERE u.uid = :uid")
    String getUserAvatarByUid(@Param("uid") Long uid);

    @Query("SELECT u.avatarResource FROM User u WHERE u.uid IN :uid")
    List<Resource> findUserAvatarByUidIn(@Param("uid") List<Long> uid);


    @Query("update User u set u.avatarResource = :avatarResourceUuid where u.uid = :uid")
    @Modifying
    int updateAvatarResourceByUid(Resource avatarResourceUuid, Long uid);

    @Query("update User u set u.avatarResource.uuid = :resourceUuid where u.uid = :uid")
    @Modifying
    int updateAvatarResourceUuidByUid(@Param("resourceUuid") String resourceUuid, Long uid);

    @Query("SELECT u.avatarResource.resourceKey FROM User u JOIN u.avatarResource WHERE u.uid in :uids")
    List<String> findUserAvatarResourceKeyUuidByUidIn(@Param("uids") Collection<Long> uids);

    @EntityGraph(attributePaths = "avatarResource")
    List<User> findAllByUidIn(List<Long> attr0);


    @Modifying
    @Query("UPDATE User u SET u.lastActiveAt = :lastActiveAt WHERE u.uid = :uid")
    int updateLastActiveAt(@Param("uid") Long uid, @Param("lastActiveAt") Instant lastActiveAt);

    @Query("""
        SELECT new org.waterwood.waterfunservicecore.entity.user.UserBriefDO(
            u.uid, u.username, u.nickname, r.resourceKey, u.level, u.userType
        ) FROM User u
        LEFT JOIN u.avatarResource r
        WHERE u.uid IN :uids
    """)
    List<UserBriefDO> findBriefDOsByUidIn(@Param("uids") List<Long> uids);
    @Query("""
        SELECT new org.waterwood.waterfunservicecore.entity.user.UserBriefDO(
            u.uid, u.username, u.nickname, r.resourceKey, u.level, u.userType
        ) FROM User u
        LEFT JOIN u.avatarResource r
        WHERE u.uid = :uid
    """)
    UserBriefDO findBriefDOsByUid(@Param("uid") Long uid);

    @Query("SELECT u.createdAt FROM User u WHERE u.uid = :uid")
    Instant getUserCreatedAtByUid(@Param("uid") Long uid);

    @Query("SELECT u.createdAt FROM User u WHERE u.uid = :uids")
    List<Instant> getUserCreatedAtByUidIn(@Param("uids") List<Long> uids);
}
