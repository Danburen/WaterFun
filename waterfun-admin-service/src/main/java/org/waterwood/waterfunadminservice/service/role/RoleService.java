package org.waterwood.waterfunadminservice.service.role;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.waterfunadminservice.api.request.role.RolePermItemDTO;
import org.waterwood.waterfunservicecore.entity.Permission;
import org.waterwood.waterfunservicecore.entity.Role;

import java.util.List;

public interface RoleService {
    /**
     * Get permissions by role ID
     * @param roleId role ID
     * @return  permissions
     */
    @Transactional(readOnly = true)
    List<Permission> getPermissions(int roleId);

    /**
     * List all the roles
     * @param spec  specification
     * @param pageable pageable
     * @return Page of role
     */
    Page<Role> listRoles(Specification<Role> spec, Pageable pageable);

    /**
     * Get role by ID
     * @param id role ID
     * @return role
     */
    Role getRole(int id);

    /**
     * Add a role, the role must not exist
     * @param entity role
     */
    Role addRole(Role entity);

    /**
     * Update role, the role ID must be set
     * @param role  role
     */
    Role updateRole(Role role);

    /**
     * Delete role
     * @param id role ID
     */
    void deleteRole(int id);

    /**
     * Assign one or more permissions to a role
     * @param id role ID
     * @param permissionIds permission IDs
     */
    void assignPerms(int id, List<RolePermItemDTO> permissionIds);

    /**
     * Update role permissions for a role
     * @param id role ID
     * @param permsDto  permission
     */
    void replaceAllRolePerms(int id, List<RolePermItemDTO> permsDto);

    /**
     * Add or update and remove permissions for a role
     * @param id role ID
     * @param updates list of updates
     * @param deletePermIds list of permission id which needs to be deleted
     */
    void changeRolePerms(int id, List<RolePermItemDTO> updates, List<Integer> deletePermIds);
}
