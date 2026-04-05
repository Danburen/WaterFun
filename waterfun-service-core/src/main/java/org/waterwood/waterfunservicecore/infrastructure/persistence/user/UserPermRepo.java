package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.user.UserPermission;
import org.waterwood.waterfunservicecore.entity.user.UserRole;

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

    @Query("SELECT up FROM UserPermission up " +
            "WHERE up.permission.id = :permId " +
            "  AND (:userUid IS NULL OR up.user.uid = :userUid) " +
            "  AND (:username IS NULL OR up.user.username LIKE %:username%) " +
            "  AND (:nickname IS NULL OR up.user.nickname LIKE %:nickname%) " +
            "ORDER BY up.createdAt DESC")
    @EntityGraph(attributePaths = "user")
    Page<UserPermission> listPermUsers(@Param("permId") int permId,
                                       @Param("userUid") Long userUid,
                                       @Param("username") String username,
                                       @Param("nickname") String nickname,
                                       Pageable pageable);

    @Query("SELECT up FROM UserPermission up " +
            "WHERE up.user.uid = :userUid " +
            " AND(:permId IS NULL OR up.permission.id = :permId) " +
            " AND(:name IS NULL OR up.permission.name LIKE %:name%) " +
            " AND(:code IS NULL OR up.permission.code LIKE %:code%) " +
            "ORDER BY up.createdAt DESC")
    @EntityGraph(attributePaths = "permission")
    Page<UserPermission> listUserPerms(@Param("userUid") long uid,
                                 @Param("permId") int permId,
                                 @Param("name") String name,
                                 @Param("code") String code,
                                 Pageable pageable);
}
