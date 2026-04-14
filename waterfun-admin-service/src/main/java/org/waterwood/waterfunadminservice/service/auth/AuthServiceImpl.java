package org.waterwood.waterfunadminservice.service.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.AuthException;
import org.waterwood.waterfunservicecore.api.req.auth.PwdLoginReq;
import org.waterwood.waterfunservicecore.api.resp.auth.LoginClientData;
import org.waterwood.waterfunservicecore.entity.Role;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserRole;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRoleRepo;
import org.waterwood.waterfunservicecore.infrastructure.utils.CookieUtil;
import org.waterwood.waterfunservicecore.services.auth.AuthCoreService;
import org.waterwood.waterfunservicecore.services.auth.LoginService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final LoginService loginService;
    private final AuthCoreService authCoreService;
    private final UserRoleRepo userRoleRepo;

    @Override
    public LoginClientData loginByPwd(PwdLoginReq body, HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        User user = loginService.login(body, CookieUtil.getCookieValue(cookies, "ADMIN_CAPTCHA_KEY"));
        List<String> roles = userRoleRepo.findByUserUid(user.getUid()).stream()
                .map(r-> {
                    return r.getRole().getCode();
                }).toList();
//        return authCoreService.BuildLoginResponse(response, user,body.getDeviceFp()).getData();
        if(roles.contains("ADMIN")) {
            return authCoreService.BuildLoginResponse(response, user,body.getDeviceFp()).getData();
        }
        throw new AuthException(BaseResponseCode.FORBIDDEN);
    }
}
