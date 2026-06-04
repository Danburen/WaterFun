package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import jakarta.persistence.NamedAttributeNode;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.user.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> , JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);

    @Modifying
    @Query("UPDATE User u SET u.passwordHash = :passwordHash WHERE u.uid = :userUid")
    int updatePassword(@Param("userUid") Long userUid,@Param("passwordHash") String passwordHash);

    List<User> getUsersByUid(Long id);

    Optional<User> findUserByUid(Long uid);
    @Query(
            "select u from User u join UserCounter c " +
                    "where u.uid in :userUids and c.visible = 1"
    )
    List<User> findAllVisibleUsersByIds(@Param("userUids") List<Long> userUids);

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
}
