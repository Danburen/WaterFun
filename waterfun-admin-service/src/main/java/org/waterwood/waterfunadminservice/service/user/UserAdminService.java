package org.waterwood.waterfunadminservice.service.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.waterfunadminservice.api.request.user.*;
import org.waterwood.waterfunadminservice.api.response.perm.AssignedPermissionRes;
import org.waterwood.waterfunadminservice.api.response.role.AssignedRoleRes;
import org.waterwood.waterfunadminservice.api.response.user.UserAdminDetail;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.common.exceptions.BizException;

import java.util.List;

public interface UserAdminService {
    User getUserByUsername(String username);

    /**
     * Get user by id
     * @param id user id
     * @throws BizException if user not found
     * @return user
     */
    User getUserById(long id);

    void deleteUser(long id);

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
     * Assign direct permissions to user (idempotent upsert semantics).
     * @param id user id
     * @param userPermItemDtos list of permission item dto
     */
    void assignPermissions(long id, List<UserPermItemDto> userPermItemDtos);

    /**
     * Remove one or more roles from user.
     * @param id user id
     * @param roleIds target role ids
     * @return batch result
     */
    BatchResult removeRoles(long id, List<Integer> roleIds);

    /**
     * Remove one or more direct permissions from user.
     * @param id user id
     * @param permissionIds target permission ids
     * @return batch result
     */
    BatchResult removePermissions(long id, List<Integer> permissionIds);

    Page<AssignedRoleRes> listAssignedRoles(long uid, Integer roleId, String code, String name, Pageable pageable);

    Page<AssignedPermissionRes> listAssignedPermissions(long uid, Integer permId, String name, String code, Pageable pageable);

    /**
     * Replace all roles to user
     * @param id user id
     * @param userRoleItemDtos list of user role item dto.
     */
    void replace(long id, List<UserRoleItemDto> userRoleItemDtos);

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

    /**
     * Get all user options for select, the option's value which id is user uid, name is username,
     *
     * @return option list
     */
    List<OptionVO<Long>> getAllUserOptions();

    /**
     * Create a new user default validated phone(won't check whether the phone number is visualise).
     * @param req request body
     */
    User createUser(CreateNewUserReq req);

    /**
     * Batch delete users
     * @param userUids the list of user uids
     * @return batch result of the operation.
     */
    BatchResult batchDeleteUsers(List<Long> userUids);
}
