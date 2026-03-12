package org.waterwood.waterfunadminservice.service.user;

import jakarta.validation.Valid;
import org.waterwood.waterfunadminservice.api.request.user.UserDatumUpdateAReq;
import org.waterwood.waterfunadminservice.api.request.user.UserInfoAUpdateReq;
import org.waterwood.waterfunadminservice.api.request.user.UserProfileUpdateAReq;
import org.waterwood.waterfunadminservice.api.request.user.UserRoleItemDto;
import org.waterwood.waterfunadminservice.api.response.user.UserAdminDetail;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.common.exceptions.BizException;

import java.util.List;

public interface UserService {
    User getUserByUsername(String username);

    /**
     * Get user by id
     * @param id user id
     * @throws BizException if user not found
     * @return user
     */
    User getUserById(long id);

    boolean activateUser(long id);

    boolean deactivateUser(long id);

    boolean suspendUser(long id);

    boolean deleteUser(long id);

    boolean isUserExist(long userId);

    User addUser(User user);

    User update(User user);

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

    /**
     * Get user detail info
     * @param uid user id
     * @return user detail info
     */
    UserAdminDetail getUserDetail(long uid);

    /**
     * Update user info
     * @param uid user id
     * @param body user info request body
     */
    void updateUserInfo(long uid, UserInfoAUpdateReq body);

    /**
     * Update user profile
     * @param uid user id
     * @param body user profile update request body
     */
    void updateUserProfile(long uid, UserProfileUpdateAReq body);

    /**
     * Update user datum
     * @param uid user id
     * @param body user datum update request body
     */
    void updateUserDatum(long uid, UserDatumUpdateAReq body);
}
