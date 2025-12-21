package org.waterwood.waterfunservicecore.services.user;

import org.waterwood.waterfunservicecore.entity.Permission;
import org.waterwood.waterfunservicecore.entity.Role;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.api.resp.user.UserInfoResponse;
import org.waterwood.common.exceptions.BusinessException;

import java.util.Set;

public interface UserCoreService {
    User getUserByUsername(String username);

    /**
     * Get user by id
     * @param uid user id
     * @throws BusinessException if user not found
     * @return userinfo response dto of {@link UserInfoResponse}
     */
    User getUserByUid(long uid);

    User addUser(User user);

    User update(User user);

    /**
     * Get user permissions
     * @param userUid user id
     * @return Set of permissions.
     */
    Set<Permission> getUserPermissions(long userUid);

    /**
     * Get user all roles
     * @param userUid user id
     * @return Set of role
     */
    Set<Role> getRoles(long userUid);

    User changePwd(long userUid, String newPwd);
    User getUser(long userUid);
}
