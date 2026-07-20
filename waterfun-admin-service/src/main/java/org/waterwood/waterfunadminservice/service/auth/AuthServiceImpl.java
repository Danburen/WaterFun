package org.waterwood.waterfunadminservice.service.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.common.exceptions.AuthException;
import org.waterwood.waterfunservicecore.api.auth.LoginResult;
import org.waterwood.waterfunservicecore.api.req.auth.PwdLoginReq;
import org.waterwood.waterfunservicecore.api.resp.auth.LoginClientData;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRoleRepo;
import org.waterwood.waterfunservicecore.infrastructure.utils.CookieUtil;
import org.waterwood.waterfunservicecore.services.auth.AuthCoreService;
import org.waterwood.waterfunservicecore.services.auth.LoginService;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final LoginService loginService;
    private final AuthCoreService authCoreService;
    private final UserRoleRepo userRoleRepo;

    @Override
    public LoginClientData loginByPwd(PwdLoginReq body, HttpServletRequest request, HttpServletResponse response) {
        return authCoreService.BuildLoginResponse(
                response,
                loginService.adminLogin(body, CookieUtil.getCookieValue(
                                request.getCookies(),
                                "ADMIN_CAPTCHA_KEY")
                        ).user(),
                body.getDeviceFp(),
                false
        );
    }
}
