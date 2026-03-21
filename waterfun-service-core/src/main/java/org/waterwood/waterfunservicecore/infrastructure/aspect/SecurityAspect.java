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

    @Before("@annotation(rr)")
    public void checkRole(RequireRole rr){
        Set<String> roles = getRoles();
        boolean pass = Arrays.stream(rr.values())
                .anyMatch(roles::contains);
        if(!pass){
            log.info("User {} need roles {} ",UserCtxHolder.getUserUid() ,roles);
            throw new BizException(BaseResponseCode.HTTP_FORBIDDEN);
        }
    }

    @Before("@annotation(rp)")
    public void checkPermission(RequirePermission rp){
        Set<String> perms = getPermissions();
        boolean pass = Arrays.stream(rp.values())
                .anyMatch(perms::contains);
        if(!pass){
            log.info("User {} need permissions {} ",UserCtxHolder.getUserUid() ,perms);
            throw new BizException(BaseResponseCode.HTTP_FORBIDDEN);
        }
    }

    private Set<String> getRoles(){
        long userUid = UserCtxHolder.getUserUid();
        Set<String> roles = redisHelper.hKeys(UserKeyBuilder.userRole(userUid)).stream()
                .map(Object::toString).collect(Collectors.toSet());
        if(roles.isEmpty()){
            roles = miss(userUid).getRoles();
        }
        return roles;
    }

    private Set<String> getPermissions(){
        long userUid = UserCtxHolder.getUserUid();
        Set<String> permissions = redisHelper.hKeys(UserKeyBuilder.userPerm(userUid)).stream()
                .map(Object::toString).collect(Collectors.toSet());
        if(permissions.isEmpty()){
            permissions = miss(userUid).getPermissions();
        }
        return permissions;
    }

    private UserAuthAttrs miss(long userUid){
        UserAuthAttrs attrs = new UserAuthAttrs();
        Set<String> roleNameSet= userCoreService.getRoles(userUid).stream()
                .map(Role::getName).collect(Collectors.toSet());
        Set<String> permCodeStream = userCoreService.getUserPermissions(userUid).stream()
                .map(Permission::getCode).collect(Collectors.toSet());
        attrs.setRoles(roleNameSet);
        attrs.setPermissions(permCodeStream);
        redisHelper.hSet(
                UserKeyBuilder.userRole(userUid), "role", roleNameSet);
        redisHelper.hSet(
                UserKeyBuilder.userPerm(userUid), "perm", permCodeStream);
        return attrs;
    }

    @Setter
    @Getter
    private static class UserAuthAttrs {
        protected Set<String> roles;
        protected Set<String> permissions;
    }
}
