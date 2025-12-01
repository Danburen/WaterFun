package org.waterwood.waterfunadminservice.service.user;

import org.waterwood.waterfunadminservice.dto.request.user.UserPwdUpdateRequestBody;
import org.waterwood.waterfunadminservice.dto.request.user.UserRoleItemDto;
import org.waterwood.waterfunadminservice.dto.response.user.UserInfoResponse;
import org.waterwood.waterfunservicecore.entity.Permission;
import org.waterwood.waterfunservicecore.entity.Role;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.common.exceptions.BusinessException;

import java.util.List;
import java.util.Set;

public interface UserService {
    User getUserByUsername(String username);

    /**
     * Get user by id
     * @param id user id
     * @throws BusinessException if user not found
     * @return userinfo response dto of {@link UserInfoResponse}
     */
    User getUserById(long id);

    boolean activateUser(long id);

    boolean deactivateUser(long id);

    boolean suspendUser(long id);

    boolean deleteUser(long id);

    boolean isUserExist(long userId);

    User addUser(User user);

    User update(User user);

    void updatePwd(UserPwdUpdateRequestBody userPwdUpdateRequestBody);

    /**
     * Get user permissions
     * @param userId user id
     * @return Set of permissions.
     */
    Set<Permission> getUserPermissions(long userId);

    /**
     * Get user all roles
     * @param userId user id
     * @return Set of role
     */
    Set<Role> getRoles(long userId);

    /**
     * Assign role to user
     * input params must not be null
     * will check the old user role record whether in the database
     * <p>If already exists</p> throw exception
     * <p>If not exists</p> will create new record
     * @param id user id
     * @param userRoleItemDtos list of role item
     */
    void assignRoles(long id, List<UserRoleItemDto> userRoleItemDtos);

    /**
     * Replace all roles to user
     * @param id user id
     * @param userRoleItemDtos list of user role item dto.
     */
    void replace(long id, List<UserRoleItemDto> userRoleItemDtos);

    /**
     * Patch user roles
     * @param id user id
     * @param adds list of user role item to add or update
     * @param deletePermIds list of permission id to delete
     */
    void change(long id, List<UserRoleItemDto> adds, List<Integer> deletePermIds);
}
