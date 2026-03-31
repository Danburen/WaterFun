package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
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
}
