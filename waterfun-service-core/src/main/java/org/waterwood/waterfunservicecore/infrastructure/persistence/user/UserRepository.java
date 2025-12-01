package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.user.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Modifying
    @Query("UPDATE User u SET u.passwordHash = :passwordHash WHERE u.id = :userId")
    int updatePassword(@Param("userId") Long userId,@Param("passwordHash") String passwordHash);

    List<User> getUsersById(Long id);

    Optional<User> findUserById(Long id);
}
