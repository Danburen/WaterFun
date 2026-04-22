package org.waterwood.waterfunservicecore.services.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.waterwood.api.enums.PermissionType;
import org.waterwood.waterfunservicecore.entity.Permission;
import org.waterwood.waterfunservicecore.entity.Role;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.api.resp.user.UserInfoResponse;
import org.waterwood.common.exceptions.BizException;

import java.time.Instant;
import java.util.Set;

public interface UserCoreService {
    User getUserByUsername(String username);

    /**
     * Get user by id
     * @param uid user id
     * @throws BizException if user not found
     * @return userinfo response dto ofPending {@link UserInfoResponse}
     */
    User getUserByUid(long uid);

    User addUser(User user);

    User update(User user);

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
     * @throws org.waterwood.waterfunservicecore.exception.NotFoundException if user not found
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
     * Get user permission
     * @param uid user id
     * @param id permission id
     * @return  permission
     */
    Permission getUserPermission(long uid, int id);

    /**
     * List users
     */
    Page<User> listUsers(String username, String nickname, String accountStatus, Instant createdStart, Instant updatedStart, Pageable pageable);

    /**
     * Update user' avatar
     *
     * @param userUid     target uid
     * @param resourceKey string key
     * @return
     */
    int updateAvatar(Long userUid, String resourceKey);

    /**
     * Return target user avatar
     * @param userUid target user uid
     * @return string of avatar null if user's avatar or user is not exists.
     */
    String getUserAvatar(Long userUid);
}
