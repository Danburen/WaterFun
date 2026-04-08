package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.Role;
import org.waterwood.waterfunservicecore.entity.RolePermission;

import java.util.Collection;
import java.util.List;

public interface RolePermRepo extends JpaRepository<RolePermission,Integer> {
    List<RolePermission> findByRole(Role role);

    List<RolePermission> findByRoleIdAndPermissionIdIn(Integer roleId, Collection<Integer> permissionIds);

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.role.id = :roleId AND rp.permission.id IN :permissionIds")
    int deleteByRoleIdAndPermissionIdIn(Integer roleId, Collection<Integer> permissionIds);

    List<RolePermission> findAllById(int id);

    void deleteByRoleId(int id);

    Page<RolePermission> findAllByRoleId(int attr0, Pageable attr1);

    @Query("SELECT rp FROM RolePermission rp " +
            "WHERE rp.role.id = :roleId " +
            "AND (:permId IS NULL OR rp.permission.id = :permId) " +
            "AND (:permName IS NULL OR rp.permission.name LIKE %:permName%) " +
            "AND (:permCode IS NULL OR rp.permission.code LIKE %:permCode%) " +
            "ORDER BY rp.createdAt DESC")
    @EntityGraph(attributePaths = "permission")
    Page<RolePermission> listRolePerms(@Param("roleId") int roleId,
                                       @Param("permId") Integer permId,
                                       @Param("permName") String name,
                                       @Param("permCode") String code,
                                       Pageable pageable);
}
