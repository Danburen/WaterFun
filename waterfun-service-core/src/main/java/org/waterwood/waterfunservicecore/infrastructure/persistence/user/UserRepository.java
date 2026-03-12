package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.user.User;

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
}
