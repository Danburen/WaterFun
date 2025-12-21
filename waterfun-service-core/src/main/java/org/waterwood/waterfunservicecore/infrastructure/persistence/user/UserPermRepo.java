package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.waterwood.waterfunservicecore.entity.user.UserPermission;

import java.util.List;
import java.util.Optional;

public interface UserPermRepo extends JpaRepository<UserPermission,Long> {
    List<UserPermission> findByUserUid(Long userUid);
    List<UserPermission> findByPermissionId(Integer permissionId);
    Optional<UserPermission> findByUserUidAndPermissionId(Long userUid, Integer permissionId);
    void deleteByUserUidAndPermissionId(Long userUid, Integer permissionId);
    boolean existsByUserUidAndPermissionId(Long userUid, Integer permissionId);
}
