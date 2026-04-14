package org.waterwood.waterfunservicecore.infrastructure.aspect;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelperHolder;
import org.waterwood.waterfunservicecore.entity.Permission;
import org.waterwood.waterfunservicecore.entity.Role;
import org.waterwood.common.exceptions.BizException;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.common.constratin.UserKeyBuilder;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Security Aspect
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SecurityAspect {
    private final RedisHelperHolder redisHelper;
    private final UserCoreService userCoreService;

    @Before("@annotation(rr) || @within(rr)")
    public void checkRoleUnified(RequireRole rr) {
        checkRole(rr);
    }

    @Before("@annotation(rr)")
    public void checkRole(RequireRole rr){
        Set<String> roles = getRoles();
        boolean pass = Arrays.stream(rr.value())
                .anyMatch(roles::contains);
        if(!pass){
            log.warn("Access denied: uid={}, hasRoles={}, required={}",
                    UserCtxHolder.getUserUid(), roles, Arrays.toString(rr.value()));
            throw new BizException(BaseResponseCode.HTTP_FORBIDDEN, 403);
        }
    }

    @Before("@annotation(rp)")
    public void checkPermission(RequirePermission rp){
        Set<String> perms = getPermissions();
        boolean pass = Arrays.stream(rp.value())
                .anyMatch(perms::contains);
        if(!pass){
            log.warn("Access denied: uid={}, hasPerms={}, required={}",
                    UserCtxHolder.getUserUid(), perms, Arrays.toString(rp.value()));
            throw new BizException(BaseResponseCode.HTTP_FORBIDDEN, 403);
        }
    }

    private Set<String> getRoles(){
        long userUid = UserCtxHolder.getUserUid();
        Set<String> roles = redisHelper.sMem(UserKeyBuilder.userRole(userUid)).stream()
                .map(Object::toString).collect(Collectors.toSet());
        if(roles.isEmpty()){
            roles = miss(userUid).getRoles();
        }
        return roles;
    }

    private Set<String> getPermissions(){
        long userUid = UserCtxHolder.getUserUid();
        Set<String> permissions = redisHelper.sMem(UserKeyBuilder.userPerm(userUid)).stream()
                .map(Object::toString).collect(Collectors.toSet());
        if(permissions.isEmpty()){
            permissions = miss(userUid).getPermissions();
        }
        return permissions;
    }

    private UserAuthAttrs miss(long userUid){
        UserAuthAttrs attrs = new UserAuthAttrs();
        Set<String> roleNameSet= userCoreService.getRoles(userUid).stream()
                .map(Role::getCode).collect(Collectors.toSet());
        Set<String> permCodeSet = userCoreService.getUserPermissions(userUid).stream()
                .map(Permission::getCode).collect(Collectors.toSet());
        attrs.setRoles(roleNameSet);
        attrs.setPermissions(permCodeSet);
        redisHelper.sAdd(
                UserKeyBuilder.userRole(userUid), roleNameSet.toArray(new String[0]));
        redisHelper.sAdd(
                UserKeyBuilder.userPerm(userUid), permCodeSet.toArray(new String[0]));
        return attrs;
    }

    @Setter
    @Getter
    private static class UserAuthAttrs {
        protected Set<String> roles;
        protected Set<String> permissions;
    }
}
