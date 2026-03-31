package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.user.UserPermission;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserPermRepo extends JpaRepository<UserPermission,Long>, JpaSpecificationExecutor<UserPermission> {
    Set<UserPermission> findByUserUid(Long userUid);
    Page<UserPermission> findByPermissionId(Integer permissionId, Pageable pageable);
    Optional<UserPermission> findByUserUidAndPermissionId(Long userUid, Integer permissionId);
    List<UserPermission> findByUserUidAndPermissionIdIn(Long userUid, Set<Integer> permissionIds);
    List<UserPermission> findByPermissionIdAndUserUidIn(Integer permissionId, List<Long> userUids);
    void deleteByUserUidAndPermissionId(Long userUid, Integer permissionId);
    int deleteByUserUidAndPermissionIdIn(Long userUid, Set<Integer> permissionIds);
    boolean existsByUserUidAndPermissionId(Long userUid, Integer permissionId);

    @Modifying
    @Query("DELETE FROM UserPermission up WHERE up.permission.id = :permissionId AND up.user.uid IN :userUids")
    int deleteByPermissionIdAndUserUidIn(@Param("permissionId") Integer permissionId, @Param("userUids") List<Long> userUids);

    int deleteByPermissionId(int id);
}
