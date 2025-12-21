package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.waterwood.waterfunservicecore.entity.user.UserRole;

import java.util.List;
import java.util.Optional;

public interface UserRoleRepo extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUserUid(Long userUid);
    List<UserRole> findByRoleId(Integer roleId);
    Optional<UserRole> findByUserUidAndRoleId(Long userUid, Integer roleId);
    boolean existsByUserUidAndRoleId(Long userUid, Integer roleId);

    void deleteByUserUid(Long userUid);

    void deleteByUserUidAndRoleIdIn(long attr0, List<Integer> attr1);
}
