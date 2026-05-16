package org.waterwood.waterfunadminservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunadminservice.api.response.user.AdminUserInfoResponse;
import org.waterwood.waterfunadminservice.service.auth.UserService;
import org.waterwood.waterfunservicecore.api.req.user.UpdateUserProfileRequest;
import org.waterwood.waterfunservicecore.services.user.UserProfileCoreService;

@RestController
@RequestMapping("/api/admin/me")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserProfileCoreService userProfileCoreService;

    @PostMapping("/info")
    public ApiResponse<AdminUserInfoResponse> getCurrentUserInfo(){
        return ApiResponse.success(
                userService.getCurrentAdminUserInfo()
        );
    }

    @PutMapping("/updateProfile")
    public ApiResponse<Void> updateProfile(@RequestBody @Valid UpdateUserProfileRequest body){
        userProfileCoreService.updateProfileByDto(body);
        return ApiResponse.success();
    }

}
