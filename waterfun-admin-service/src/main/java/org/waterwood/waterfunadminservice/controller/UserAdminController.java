package org.waterwood.waterfunadminservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.waterfunadminservice.api.request.user.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunadminservice.api.response.perm.AssignedPermissionRes;
import org.waterwood.waterfunadminservice.api.response.role.AssignedRoleRes;
import org.waterwood.waterfunadminservice.api.response.user.UserAdminDetail;
import org.waterwood.waterfunadminservice.api.response.user.UserInfoARes;
import org.waterwood.waterfunadminservice.infrastructure.mapper.UserAdminMapper;
import org.waterwood.waterfunadminservice.service.user.UserService;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class UserAdminController {
    private final UserService userService;
    private final UserCoreService userCoreService;
    private final UserAdminMapper userAdminMapper;

    @Operation(summary = "List users")
    @GetMapping("/list")
    public ApiResponse<Page<UserInfoARes>> list(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String accountStatus,
            @RequestParam(required = false) Instant createdStart,
            @RequestParam(required = false) Instant createdEnd,
            @PageableDefault Pageable pageable){
        Page<User> users = userCoreService.listUsers(username, nickname, accountStatus, createdStart, createdEnd, pageable);
        return ApiResponse.success(
                users.map(userAdminMapper::toDto)
        );
    }

    @Operation(summary = "Get user detail")
    @GetMapping("/{uid}")
    public ApiResponse<UserAdminDetail> getUserDetail(@PathVariable long uid){
       UserAdminDetail detail = userService.getUserDetail(uid);
       return ApiResponse.success(detail);
    }

    @Operation(summary = "Delete user")
    @DeleteMapping("/{uid}")
    public ApiResponse<Void> deleteUser(@PathVariable long uid){
        userService.deleteUser(uid);
        return ApiResponse.success();
    }

    @Operation(summary = "Assign roles to user")
    @PostMapping("/{uid}/roles")
    public ApiResponse<Void> assignRoleToUser(@PathVariable long uid, @Valid @RequestBody AssignUserRoleReq body){
        userService.assignRoles(uid, body.getUserRoles());
        return ApiResponse.success();
    }

    @Operation(summary = "Assign single role to user")
    @PostMapping("/{uid}/role")
    public ApiResponse<Void> assignSingleRoleToUser(@PathVariable long uid, @Valid @RequestBody AssignSingleUserRoleReq body){
        UserRoleItemDto item = new UserRoleItemDto();
        item.setRoleId(body.getRoleId());
        item.setExpiresAt(body.getExpiresAt());
        userService.assignRoles(uid, java.util.List.of(item));
        return ApiResponse.success();
    }

    @Operation(summary = "Remove single role from user")
    @DeleteMapping("/{uid}/role/{roleId}")
    public ApiResponse<BatchResult> removeSingleRoleFromUser(@PathVariable long uid, @PathVariable int roleId){
        return ApiResponse.success(userService.removeRoles(uid, List.of(roleId)));
    }

    @Operation(summary = "Full Replace roles of user")
    @PutMapping("/{uid}/roles")
    public ApiResponse<Void> updateRoleToUser(@PathVariable long uid, @Valid @RequestBody UpdateUserRoleReq body){
        userService.replace(uid, body.getUserRoleItemDtos());
        return ApiResponse.success();
    }

    @Operation(summary = "List user roles")
    @GetMapping("/{uid}/roles")
    public ApiResponse<Page<AssignedRoleRes>> listUserRoles(@PathVariable long uid,
                                                            @RequestParam(required = false) Integer roleId,
                                                            @RequestParam(required = false) String name,
                                                            @RequestParam(required = false) String code,
                                                            @PageableDefault Pageable pageable){
        return ApiResponse.success(userService.listAssignedRoles(uid, roleId, code, name, pageable));
    }

    @Operation(summary = "List user permissions")
    @GetMapping("/{uid}/permissions")
    public ApiResponse<Page<AssignedPermissionRes>> listUserPermissions(@PathVariable long uid,
                                                                        @RequestParam(required = false) Integer permId,
                                                                        @RequestParam(required = false) String name,
                                                                        @RequestParam(required = false) String code,
                                                                        @PageableDefault Pageable pageable){
        return ApiResponse.success(userService.listAssignedPermissions(uid, permId, name, code, pageable));
    }

    @Operation(summary = "Assign direct permissions to user")
    @PostMapping("/{uid}/permissions")
    public ApiResponse<Void> assignPermsToUser(@PathVariable long uid, @Valid @RequestBody AssignUserPermReq body){
        userService.assignPermissions(uid, body.getUserPermissions());
        return ApiResponse.success();
    }

    @Operation(summary = "Assign single direct permission to user")
    @PostMapping("/{uid}/permission")
    public ApiResponse<Void> assignSinglePermToUser(@PathVariable long uid, @Valid @RequestBody AssignSingleUserPermReq body){
        UserPermItemDto item = new UserPermItemDto();
        item.setPermissionId(body.getPermissionId());
        item.setExpiresAt(body.getExpiresAt());
        userService.assignPermissions(uid, java.util.List.of(item));
        return ApiResponse.success();
    }

    @Operation(summary = "Remove single direct permission from user")
    @DeleteMapping("/{uid}/permission/{permissionId}")
    public ApiResponse<BatchResult> removeSinglePermFromUser(@PathVariable long uid, @PathVariable int permissionId){
        return ApiResponse.success(userService.removePermissions(uid, List.of(permissionId)));
    }

    @Operation(summary = "Batch remove roles from user")
    @DeleteMapping("/{uid}/roles")
    public ApiResponse<BatchResult> removeRolesFromUser(@PathVariable long uid, @Valid @RequestBody RemoveUserRolesReq body){
        return ApiResponse.success(userService.removeRoles(uid, body.getRoleIds()));
    }

    @Operation(summary = "Batch remove direct permissions from user")
    @DeleteMapping("/{uid}/permissions")
    public ApiResponse<BatchResult> removePermsFromUser(@PathVariable long uid, @Valid @RequestBody RemoveUserPermsReq body){
        return ApiResponse.success(userService.removePermissions(uid, body.getPermissionIds()));
    }

    @Operation(summary = "Update user info")
    @PutMapping("/{uid}/info")
    public ApiResponse<Void> updateUserInfo(@PathVariable long uid, @Valid @RequestBody UserInfoAUpdateReq body){
        userService.updateUserInfo(uid, body);
        return ApiResponse.success();
    }

    @Operation(summary = "Update user profile")
    @PutMapping("/{uid}/profile")
    public ApiResponse<Void> updateUserProfile(@PathVariable long uid, @Valid @RequestBody UserProfileUpdateAReq body){
        userService.updateUserProfile(uid, body);
        return ApiResponse.success();
    }

    @Operation(summary = "Update user datum")
    @PutMapping("/{uid}/datum")
    public ApiResponse<Void> updateUserDatum(@PathVariable long uid, @Valid @RequestBody UserDatumUpdateAReq body){
        userService.updateUserDatum(uid, body);
        return ApiResponse.success();
    }

    @GetMapping("/options")
    public ApiResponse<List<OptionVO<Long>>> getUserOptions() {
        return ApiResponse.success(
                userService.getAllUserOptions()
        );
    }

}
