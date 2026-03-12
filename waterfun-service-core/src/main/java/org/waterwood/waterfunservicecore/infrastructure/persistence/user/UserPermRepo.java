package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.waterwood.waterfunservicecore.entity.user.UserPermission;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserPermRepo extends JpaRepository<UserPermission,Long>, JpaSpecificationExecutor<UserPermission> {
    Set<UserPermission> findByUserUid(Long userUid);
    List<UserPermission> findByPermissionId(Integer permissionId);
    Optional<UserPermission> findByUserUidAndPermissionId(Long userUid, Integer permissionId);
    void deleteByUserUidAndPermissionId(Long userUid, Integer permissionId);
    boolean existsByUserUidAndPermissionId(Long userUid, Integer permissionId);
}
