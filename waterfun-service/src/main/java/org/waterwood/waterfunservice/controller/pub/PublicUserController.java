package org.waterwood.waterfunservice.controller.pub;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservice.api.response.UserPublicCardResp;
import org.waterwood.waterfunservice.api.response.UserPublicInfoResp;
import org.waterwood.waterfunservice.api.response.UserPublicProfileResp;
import org.waterwood.waterfunservice.service.user.UserService;
import org.waterwood.waterfunservicecore.api.req.user.ListUserAvatarsReq;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.infrastructure.mapper.UserCoreMapper;
import org.waterwood.waterfunservicecore.services.user.UserProfileCoreService;

import java.util.Map;

@RestController
@RequestMapping("/api/public/user")
@RequiredArgsConstructor
public class PublicUserController {
    private final UserCoreMapper userCoreMapper;
    private final UserProfileCoreService userProfileCoreService;
    private UserService userService;

    @Operation(summary = "获取用户的公开基本信息")
    @RequestMapping("/{userUid}/info")
    public ApiResponse<UserPublicInfoResp> getUserInfo(@PathVariable long userUid) {
        return ApiResponse.success(
                userService.getPublicUserInfo(userUid)
        );
    }
    @Operation(summary = "获取用户的公开资料")
    @RequestMapping("/{userUid}/profile")
    public ApiResponse<UserPublicProfileResp> getUserProfile(@PathVariable long userUid) {
        return ApiResponse.success(
                userService.getPublicUserProfile(userUid)
        );
    }

    @Operation(summary = "获取用户的公开卡片")
    @RequestMapping("/{userUid}/card")
    public ApiResponse<UserPublicCardResp> getUserCard(@PathVariable long userUid) {
        return ApiResponse.success(
                userService.getPublicUserCard(userUid)
        );
    }
    @Operation(summary = "获取用户的粉丝列表")
    @RequestMapping("/{userUid}/follower")
    public ApiResponse<Page<UserPublicCardResp>> getFollower(@PathVariable long userUid,
                                                              @RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return ApiResponse.success(
                userService.listUserFollowers(userUid, pageable)
        );
    }
    
    @Operation(summary = "获取用户的关注列表")
    @RequestMapping("/{userUid}/following")
    public ApiResponse<Page<UserPublicCardResp>> getFollowing(@PathVariable long userUid,
                                                              @RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "10") int size){
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return ApiResponse.success(
                userService.listUserFollowing(userUid, pageable)
        );
    }

    @Operation(summary = "获取用户的头像")
    @RequestMapping("/{userUid}/avatar")
    public ApiResponse<CloudResPresignedUrlResp> getUserAvatar(@PathVariable long userUid) {
        return ApiResponse.success(
                userProfileCoreService.getUserAvatar(userUid)
        );
    }

    public ApiResponse<Map<String, CloudResPresignedUrlResp>> listUserAvatars(@Valid @RequestBody ListUserAvatarsReq req){
        userProfileCoreService.listUserAvatars(req.getUserUids());
        return ApiResponse.success(

        );
    }

}
