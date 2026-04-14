package org.waterwood.waterfunadminservice.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunadminservice.api.response.user.AdminUserInfoResponse;
import org.waterwood.waterfunservicecore.api.resp.user.UserInfoResponse;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserPermission;
import org.waterwood.waterfunservicecore.entity.user.UserRole;
import org.waterwood.waterfunservicecore.infrastructure.mapper.UserCoreMapper;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;
import org.waterwood.waterfunservicecore.services.user.UserPermissionCoreService;
import org.waterwood.waterfunservicecore.services.user.UserProfileCoreService;
import org.waterwood.waterfunservicecore.services.user.UserRoleCoreService;

import java.util.List;

@Service
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserServiceImpl implements UserService {
    private final UserCoreService userCoreService;
    private final UserCoreMapper userCoreMapper;
    private final UserProfileCoreService userProfileCoreService;
    private final UserRoleCoreService userRoleCoreService;
    private final UserPermissionCoreService userPermissionCoreService;

    @Override
    public AdminUserInfoResponse getCurrentAdminUserInfo() {
        User u = userCoreService.getUserByUid(UserCtxHolder.getUserUid());
        UserInfoResponse plainRes = userCoreMapper.toUserInfoResponse(u);
        AdminUserInfoResponse res = new AdminUserInfoResponse();
        res.setUid(plainRes.getUid());
        res.setUsername(plainRes.getUsername());
        res.setNickname(plainRes.getNickname());
        res.setAccountStatus(plainRes.getAccountStatus());
        res.setCreatedAt(plainRes.getCreatedAt());
        res.setAvatar(userProfileCoreService.getUserAvatar(u.getUid()));
        res.setPasswordHash(u.getPasswordHash() != null);
        res.setRoles(userRoleCoreService.getUserRoles(u.getUid()).stream().map(UserRole::getCode).toList());
        res.setPermissions(userPermissionCoreService.getUserPermission(u.getUid()).stream().map(UserPermission::getCode).toList());
        return res;
    }
}
