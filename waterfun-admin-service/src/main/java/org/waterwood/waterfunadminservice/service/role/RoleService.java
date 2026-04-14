package org.waterwood.waterfunadminservice.service.role;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.waterfunadminservice.api.request.role.*;
import org.waterwood.waterfunadminservice.api.response.perm.AssignedPermissionRes;
import org.waterwood.waterfunadminservice.api.response.user.AssignedUserRes;
import org.waterwood.waterfunservicecore.entity.Permission;
import org.waterwood.waterfunservicecore.entity.Role;

import java.time.Instant;
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
     * Add a role, the role must not existsWithUniqueIdentify
     *
     * @param req role
     */
    Role addRole(CreateRoleRequest req);

    /**
     * Update role, the role ID must be set
     *
     * @param id
     * @param req role
     */
    Role updateRole(int id, UpdateRoleRequest req);

    /**
     * Delete role
     * @param id role ID
     */
    void deleteRole(int id);

    /**
     * Assign one or more permissions to a role
     *
     * @param id            role ID
     * @param permissionIds permission IDs
     * @return batch result
     */
    BatchResult assignPerms(int id, List<RolePermItemDTO> permissionIds);

    /**
     * Update role permissions for a role
     * @param id role ID
     * @param permsDto  permission
     */
     BatchResult replaceAllRolePerms(int id, List<RolePermItemDTO> permsDto);

    /**
     * List the permissions of a role with pagination
     *
     * @param id       the role id
     * @param permId   permission Id
     * @param code     permission code
     * @param name     permission name
     * @param pageable pageable
     * @return page of permissions
     */
    Page<AssignedPermissionRes> getRolePerms(int id, Integer permId, String code, String name, Pageable pageable);

    /**
     * Assign role to user
     *
     * @param id       role ID
     * @param userIds  the request body
     * @param expireAt role expire time ,if set null, will never expire
     * @return Batch Result
     */
    BatchResult assignUsers(int id, List<Long> userIds, Instant expireAt);

    /**
     * List users of a role with pagination
     *
     * @param id       role id
     * @param userUid user UID
     * @param username username
     * @param nickname nickname
     * @param pageable pageable
     * @return page of user
     */
    Page<AssignedUserRes> getRoleUsers(int id, Long userUid, String username, String nickname, Pageable pageable);

    /**
     * Remove users role which have target roles
     *
     * @param id                the role id
     * @param removeRoleUserIds target user ids
     * @return batch result
     */
    BatchResult removeRoleUsers(int id, List<Long> removeRoleUserIds);

    /**
     * Remove a role's permissions
     *
     * @param id      the role id
     * @param permIds target permission ids
     * @return batch result
     */
    BatchResult removeRolePerms(int id, List<Integer> permIds);

    /**
     * Batch replacing users of a role, the users will be replaced by the given user list, and the old users will be removed
     * @param id the role id
     * @param userUids user uids
     * @param expiresAt role expire time ,if set null, will never expire
     * @return batch processing result
     */
    BatchResult replaceUserRoles(int id, List<Long> userUids, Instant expiresAt);

    /**
     * Get all role ids.
     *
     * @return list of role ids
     */
    List<OptionVO<Integer>> getAllRoleOptions();

    /**
     * Batch remove roles
     * @param req request body
     * @return batch result of the operation.
     */
    BatchResult removeRoles(DeleteRolesRequest req);
}
