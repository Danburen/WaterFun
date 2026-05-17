package org.waterwood.waterfunservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.waterwood.waterfunservice.service.user.UserProfileService;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.req.user.UpdateUserProfileRequest;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservicecore.api.resp.user.UserInfoResponse;
import org.waterwood.waterfunservicecore.api.resp.user.UserProfileResponse;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.Permission;
import org.waterwood.waterfunservicecore.infrastructure.aspect.RateLimit;
import org.waterwood.waterfunservicecore.infrastructure.mapper.UserCoreMapper;
import org.waterwood.waterfunservicecore.infrastructure.mapper.UserProfileCoreMapper;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.user.UserProfileCoreServiceImpl;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserCoreService userCoreService;
    private final UserProfileCoreServiceImpl userCoreProfileService;
    private final UserCoreMapper userCoreMapper;
    private final UserProfileCoreMapper userProfileCoreMapper;
    private final CloudFileService cloudFileService;
    private final UserProfileService userProfileService;

    @GetMapping("/userInfo")
    public ApiResponse<UserInfoResponse> getUserInfo(){
        User user = userCoreService.getUserByUid(UserCtxHolder.getUserUid());
        UserInfoResponse res = userCoreMapper.toUserInfoResponse(user);
        res.setAvatar(userCoreProfileService.getUserAvatar(user.getUid()));
        res.setPasswordHash(user.getPasswordHash() != null);
        return ApiResponse.success(res);
    }
    @PutMapping("/updateProfile")
    public ApiResponse<Void> updateProfile(@RequestBody @Valid UpdateUserProfileRequest body){
        userCoreProfileService.updateProfileByDto(body);
        return ApiResponse.success();
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getProfile(){
        UserProfile up = userCoreProfileService.getUserProfile(UserCtxHolder.getUserUid());
        UserProfileResponse res = userProfileCoreMapper.toResponse(up);
        return ApiResponse.success(res);
    }


//    @RateLimit(key = "avatarUpload", permits = 5)
//    @GetMapping("/avatar/upload")
//    public ApiResponse<PresignedResp> updateAvatar(@RequestParam @Valid @NotEmpty String suffix){
//        return ApiResponse.success(
//                userProfileService.getUploadPolicyAndSubmitAvatar(UserCtxHolder.getUserUid(), suffix)
//        );
//    }
//
//    @PostMapping("/avatar/upload/callback")
//    public ApiResponse<Void> updateAvatarCallback(@RequestBody CloudPutCallbackReq req){
//        userProfileService.uploadAvatarCallback(req, );
//        return ApiResponse.success();
//    }

    @GetMapping("/avatar")
    public ApiResponse<CloudResPresignedUrlResp> getAvatar(){
        return ApiResponse.success(
                userCoreProfileService.getUserAvatar(UserCtxHolder.getUserUid())
        );
    }


    @GetMapping("/permissions")
    public ApiResponse<Set<String>> getPermissions(){
        long userUid = UserCtxHolder.getUserUid();
        Set<String> permCodes = userCoreService.getUserPermissions(userUid)
                .stream().map(Permission::getCode)
                .collect(Collectors.toSet());
        return ApiResponse.success(permCodes);
    }
}
