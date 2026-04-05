package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.user.UserRole;

import java.util.List;
import java.util.Set;

public interface UserRoleRepo extends JpaRepository<UserRole, Long>, JpaSpecificationExecutor<UserRole> {
    Set<UserRole> findByUserUid(Long userUid);

    List<UserRole> findByRoleIdAndUserUidIn(Integer roleId, List<Long> userUids);
    List<UserRole> findByUserUidAndRoleIdIn(Long userUid, Set<Integer> roleIds);

    int deleteByUserUidAndRoleIdIn(Long userUid, Set<Integer> roleIds);

    void deleteByUserUid(Long userUid);

    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.role.id = :id AND ur.user.uid IN :userUids")
    int deleteByRoleIdAndUserUids(@Param("id") int id,@Param("userUids") List<Long> userUids);

    int deleteByRoleId(Integer roleId);

    @Query("SELECT ur FROM UserRole ur " +
            "WHERE ur.role.id = :roleId " +
            "  AND (:userUid IS NULL OR ur.user.uid = :userUid) " +
            "  AND (:username IS NULL OR ur.user.username LIKE %:username%) " +
            "  AND (:nickname IS NULL OR ur.user.nickname LIKE %:nickname%) " +
            "ORDER BY ur.createdAt DESC")
    @EntityGraph(attributePaths = "user")
    Page<UserRole> listRoleUsers(@Param("roleId") int id,
                                     @Param("userUid") Long userUid,
                                     @Param("username") String username,
                                     @Param("nickname") String nickname,
                                     Pageable pageable);

    @Query("SELECT ur FROM UserRole ur " +
            "WHERE ur.user.uid = :userUid " +
            " AND(:roleId IS NULL OR ur.role.id = :roleId) " +
            " AND(:name IS NULL OR ur.role.name LIKE %:name%) " +
            " AND(:code IS NULL OR ur.role.code LIKE %:code%) " +
            "ORDER BY ur.createdAt DESC")
    @EntityGraph(attributePaths = "role")
    Page<UserRole> listUserRoles(@Param("userUid") long uid,
                                 @Param("roleId") Integer roleId,
                                 @Param("code") String code,
                                 @Param("name") String name,
                                 Pageable pageable);
}
