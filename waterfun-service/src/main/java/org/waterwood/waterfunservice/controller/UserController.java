package org.waterwood.waterfunservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
import org.waterwood.waterfunservicecore.api.resp.user.UserPublicCardResp;
import org.waterwood.waterfunservice.api.response.UserPublicProfileResp;
import org.waterwood.waterfunservice.service.user.UserService;
import org.waterwood.waterfunservicecore.api.req.user.UpdateUserProfileRequest;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservicecore.api.resp.user.UserInfoResponse;
import org.waterwood.waterfunservicecore.api.resp.user.UserProfileResponse;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.Permission;
import org.waterwood.waterfunservicecore.exception.notfound.UserNotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.mapper.UserCoreMapper;
import org.waterwood.waterfunservicecore.infrastructure.mapper.UserProfileCoreMapper;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.user.UserProfileCoreService;
import org.waterwood.waterfunservicecore.services.user.UserProfileCoreServiceImpl;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;

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
    private final UserService userService;
    private final UserProfileCoreService userProfileCoreService;
    private final UserRepository userRepository;

    @Operation(summary = "Get current user info")
    @GetMapping("/userInfo")
    public ApiResponse<UserInfoResponse> getUserInfo(){
        User user = userRepository.findById(UserCtxHolder.getUserUid())
                .orElseThrow(() -> new UserNotFoundException(UserCtxHolder.getUserUid()));
        UserInfoResponse res = userCoreMapper.toUserInfoResponse(user);
        res.setAvatar(userCoreProfileService.getUserAvatar(user.getUid()));
        res.setPasswordHash(user.getPasswordHash() != null);
        return ApiResponse.success(res);
    }

    @Operation(summary = "Update current user profile")
    @PutMapping("/updateProfile")
    public ApiResponse<Void> updateProfile(@RequestBody @Valid UpdateUserProfileRequest body){
        userCoreProfileService.updateProfileByDto(body);
        return ApiResponse.success();
    }

    @Operation(summary = "Get current user profile")
    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getProfile(){
        UserProfile up = userCoreProfileService.getUserProfile(UserCtxHolder.getUserUid());
        UserProfileResponse res = userProfileCoreMapper.toResponse(up);
        return ApiResponse.success(res);
    }

    @Operation(summary = "Get current user avatar")
    @GetMapping("/avatar")
    public ApiResponse<CloudResPresignedUrlResp> getAvatar(){
        return ApiResponse.success(
                userCoreProfileService.getUserAvatar(UserCtxHolder.getUserUid())
        );
    }

    @Operation(summary = "Get current user permissions")
    @GetMapping("/permissions")
    public ApiResponse<Set<String>> getPermissions(){
        long userUid = UserCtxHolder.getUserUid();
        Set<String> permCodes = userCoreService.getUserPermissions(userUid)
                .stream().map(Permission::getCode)
                .collect(Collectors.toSet());
        return ApiResponse.success(permCodes);
    }

    @Operation(summary = "Get user public profile")
    @RequestMapping("/{uid}/profile")
    public ApiResponse<UserPublicProfileResp> getUserProfile(@PathVariable long uid) {
        return ApiResponse.success(
                userService.getPublicUserProfile(uid)
        );
    }

    @Operation(summary = "Get user public card")
    @RequestMapping("/{uid}/card")
    public ApiResponse<UserPublicCardResp> getUserCard(@PathVariable long uid) {
        return ApiResponse.success(
                userService.getPublicUserCard(uid)
        );
    }
    @Operation(summary = "Get the followers list of a user")
    @RequestMapping("/{uid}/followers")
    public ApiResponse<Page<UserBrief>> getFollower(@PathVariable long uid,
                                                             @RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return ApiResponse.success(
                userService.listUserFollowers(uid, pageable)
        );
    }

    @Operation(summary = "Get the following list of a user")
    @RequestMapping("/{uid}/followings")
    public ApiResponse<Page<UserBrief>> getFollowing(@PathVariable long uid,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int size){
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return ApiResponse.success(
                userService.listUserFollowing(uid, pageable)
        );
    }

    @Operation(summary = "Get a user avatar")
    @RequestMapping("/{uid}/avatar")
    public ApiResponse<CloudResPresignedUrlResp> getUserAvatar(@PathVariable long uid) {
        return ApiResponse.success(
                userProfileCoreService.getUserAvatar(uid)
        );
    }

    @Operation(summary = "Follow/ unFollower a user",
            description = "Follow a user if not followed, otherwise unFollow the user")
    @RequestMapping("/{uid}/follow")
    public ApiResponse<Void> follow(@PathVariable long uid) {
        userService.follow(uid);
        return ApiResponse.success();
    }
}
