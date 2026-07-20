package org.waterwood.waterfunadminservice.service.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunadminservice.api.request.AdminChangePasswordReq;
import org.waterwood.waterfunservicecore.api.auth.LoginResult;
import org.waterwood.waterfunservicecore.api.req.auth.PwdLoginReq;
import org.waterwood.waterfunservicecore.api.resp.auth.LoginClientData;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogActionType;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRoleRepo;
import org.waterwood.waterfunservicecore.infrastructure.utils.CookieUtil;
import org.waterwood.waterfunservicecore.services.audit.AuditLogCoreService;
import org.waterwood.waterfunservicecore.services.auth.AuthCoreService;
import org.waterwood.waterfunservicecore.services.auth.LoginService;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final LoginService loginService;
    private final AuthCoreService authCoreService;
    private final UserCoreService userCoreService;
    private final AuditLogCoreService auditLogCoreService;
    private final UserRoleRepo userRoleRepo;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

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

    @Override
    @Transactional
    public void adminChangePassword(long uid, AdminChangePasswordReq dto) {
        try {
            User admin = userCoreService.getUser(uid);

            if (!encoder.matches(dto.getOldPwd(), admin.getPasswordHash())) {
                throw new BizException(BaseResponseCode.OLD_PASSWORD_INCORRECT);
            }
            if (!dto.getNewPwd().equals(dto.getConfirmPwd())) {
                throw new BizException(BaseResponseCode.PASSWORD_TWO_PASSWORD_NOT_EQUAL);
            }

            userCoreService.changePwd(uid, dto.getNewPwd());
        } catch (Exception e) {
            auditLogCoreService.recordFailure(uid, null, AuditLogActionType.CHANGE_PASSWORD, e.getMessage());
            throw e;
        }
    }
}
