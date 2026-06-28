package org.waterwood.waterfunadminservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunadminservice.api.response.user.AdminUserInfoResponse;
import org.waterwood.waterfunadminservice.service.auth.UserService;
import org.waterwood.waterfunservicecore.api.req.user.UpdateUserProfileRequest;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.user.UserPermission;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;
import org.waterwood.waterfunservicecore.entity.user.UserRole;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.user.UserPermissionCoreService;
import org.waterwood.waterfunservicecore.services.user.UserProfileCoreService;
import org.waterwood.waterfunservicecore.services.user.UserRoleCoreService;

import java.util.Set;

@RestController
@RequestMapping("/api/admin/me")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserProfileCoreService userProfileCoreService;
    private final UserRoleCoreService userRoleCoreService;
    private final UserPermissionCoreService userPermissionCoreService;

    @GetMapping("/info")
    public ApiResponse<AdminUserInfoResponse> getCurrentUserInfo(){
        return ApiResponse.success(
                userService.getCurrentAdminUserInfo()
        );
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfile> getProfile() {
        return ApiResponse.success(
                userProfileCoreService.getUserProfile(UserCtxHolder.getUserUid())
        );
    }

    @GetMapping("/roles")
    public ApiResponse<Set<UserRole>> getRoles() {
        return ApiResponse.success(
                userRoleCoreService.getUserRoles(UserCtxHolder.getUserUid())
        );
    }

    @GetMapping("/permissions")
    public ApiResponse<Set<UserPermission>> getPermissions() {
        return ApiResponse.success(
                userPermissionCoreService.getUserPermission(UserCtxHolder.getUserUid())
        );
    }

    @GetMapping("/avatar")
    public ApiResponse<CloudResPresignedUrlResp> getAvatar() {
        return ApiResponse.success(
                userProfileCoreService.getUserAvatar(UserCtxHolder.getUserUid())
        );
    }

    @PutMapping("/updateProfile")
    public ApiResponse<Void> updateProfile(@RequestBody @Valid UpdateUserProfileRequest body){
        userProfileCoreService.updateProfileByDto(body);
        return ApiResponse.success();
    }

}
