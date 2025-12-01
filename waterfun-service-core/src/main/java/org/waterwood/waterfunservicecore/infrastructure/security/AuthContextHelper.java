package org.waterwood.waterfunservicecore.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.BusinessException;
import org.waterwood.common.exceptions.ServiceException;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserContext;

public final class AuthContextHelper {
    public static Jwt getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof Jwt) {
            return (Jwt) authentication.getDetails();
        }
        return null;
    }

    public static String getCurrentClaim(String claimName) {
        Jwt jwt = getCurrentJwt();
        if(jwt == null){
            throw new BusinessException(BaseResponseCode.HTTP_UNAUTHORIZED);
        }
        if(jwt.getClaim(claimName) == null){
            throw new ServiceException("Claim not found");
        }
        return  jwt.getClaim(claimName);
    }

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null && authentication.getPrincipal() instanceof UserContext ctx){
            return ctx.getUserId();
        }else{
            throw new BusinessException(BaseResponseCode.HTTP_UNAUTHORIZED);
        }
    }
}
