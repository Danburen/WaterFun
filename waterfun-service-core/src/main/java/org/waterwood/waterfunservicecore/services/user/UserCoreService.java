package org.waterwood.waterfunservicecore.services.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.waterwood.waterfunservicecore.entity.perm.PermissionType;
import org.waterwood.waterfunservicecore.entity.perm.Permission;
import org.waterwood.waterfunservicecore.entity.user.Role;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.exception.notfound.NotFoundException;

import java.time.Instant;
import java.util.Set;

public interface UserCoreService {

    /**
     * Get user permissions
     * @param userUid user id
     * @return Set ofPending permissions.
     */
    Set<Permission> getUserPermissions(long userUid);

    /**
     * Get user all roles
     * @param userUid user id
     * @return Set ofPending role
     */
    Set<Role> getRoles(long userUid);

    User changePwd(long userUid, String newPwd);

    /**
     * Get a user by user uid
     * @param userUid target userUid
     * @return user entity
     * @throws NotFoundException if user not found
     */
    User getUser(long userUid);

    /**
     * List user roles
     * @param uid user's uid
     * @param roleName role name
     * @param roleParent role parent
     * @param pageable pageable
     * @return page ofPending roles
     */
    Page<Role> listRoles(long uid, String roleName, Integer roleParent, Pageable pageable);

    /**
     * List user permissions
     * @param uid user id
     * @param name permission name
     * @param code permission code
     * @param resource permission resource
     * @param type permission type
     * @param pageable pageable
     * @return page ofPending permissions
     */
    Page<Permission> listPermissions(long uid, String name, String code, String resource, PermissionType type, Integer parentId,Pageable pageable);

    /**
     * List users
     */
    Page<User> listUsers(String username, String nickname, String accountStatus, Instant createdStart, Instant updatedStart, Pageable pageable);

    /**
     * Update user' ofUser
     *
     * @param userUid     target uid
     * @param uuid        target resource uuid
     * @return
     */
    int updateAvatarResourceUuid(Long userUid, String uuid);

    /**
     * Return target user ofUser
     * @param userUid target user uid
     * @return string of ofUser null if user's ofUser or user is not exists.
     */
    String getUserAvatar(Long userUid);

    /**
     * Whether a user is admin
     */
    boolean isUserAdmin(Long userUid);

}
