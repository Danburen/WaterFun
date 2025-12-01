package org.waterwood.waterfunadminservice.infrastructure.aspect;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.entity.Permission;
import org.waterwood.waterfunservicecore.entity.Role;
import org.waterwood.common.exceptions.BusinessException;
import org.waterwood.waterfunservicecore.infrastructure.security.AuthContextHelper;
import org.waterwood.waterfunadminservice.service.user.UserService;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelper;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Security Aspect
 */
@Slf4j
@Aspect
@Component
public class SecurityAspect {
    private final RedisHelper redisHelper;
    private static final String REDIS_KEY_PREFIX = "user:";
    private final UserService userService;

    public SecurityAspect(RedisHelper redisHelper, UserService userService){
        this.redisHelper = redisHelper;
        redisHelper.setKeyPrefix(REDIS_KEY_PREFIX);
        this.userService = userService;
    }
    @Before("@annotation(rr)")
    public void checkRole(RequireRole rr){
        Set<String> roles = getRoles();
        boolean pass = Arrays.stream(rr.values())
                .anyMatch(roles::contains);
        if(!pass){
            log.info("User {} need roles {} ", AuthContextHelper.getCurrentUserId() ,roles);
            throw new BusinessException(BaseResponseCode.HTTP_FORBIDDEN);
        }
    }

    @Before("@annotation(rp)")
    public void checkPermission(RequirePermission rp){
        Set<String> perms = getPermissions();
        boolean pass = Arrays.stream(rp.values())
                .anyMatch(perms::contains);
        if(!pass){
            log.info("User {} need permissions {} ", AuthContextHelper.getCurrentUserId() ,perms);
            throw new BusinessException(BaseResponseCode.HTTP_FORBIDDEN);
        }
    }

    private Set<String> getRoles(){
        long userId = AuthContextHelper.getCurrentUserId();
        Set<String> roles = redisHelper.hKeys("role:" + userId).stream()
                .map(Object::toString).collect(Collectors.toSet());
        if(roles.isEmpty()){
            roles = miss(userId).getRoles();
        }
        return roles;
    }

    private Set<String> getPermissions(){
        long userId = AuthContextHelper.getCurrentUserId();
        Set<String> permissions = redisHelper.hKeys("perm:" + userId).stream()
                .map(Object::toString).collect(Collectors.toSet());
        if(permissions.isEmpty()){
            permissions = miss(userId).getPermissions();
        }
        return permissions;
    }

    private UserAuthAttrs miss(long userId){
        UserAuthAttrs attrs = new UserAuthAttrs();
        Set<String> roleNameSet= userService.getRoles(userId).stream()
                .map(Role::getName).collect(Collectors.toSet());
        Set<String> permCodeStream = userService.getUserPermissions(userId).stream()
                .map(Permission::getCode).collect(Collectors.toSet());
        attrs.setRoles(roleNameSet);
        attrs.setPermissions(permCodeStream);
        redisHelper.hSet(Long.toString(userId), "role", roleNameSet);
        redisHelper.hSet(Long.toString(userId), "perm", permCodeStream);
        return attrs;
    }

    @Setter
    @Getter
    private static class UserAuthAttrs {
        protected Set<String> roles;
        protected Set<String> permissions;
    }
}
