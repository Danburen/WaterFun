package org.waterwood.waterfunadminservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.api.TokenPair;
import org.waterwood.waterfunadminservice.api.request.AdminChangePasswordReq;
import org.waterwood.waterfunadminservice.service.auth.AuthService;
import org.waterwood.waterfunadminservice.service.auth.UserService;
import org.waterwood.waterfunservicecore.api.req.auth.LogoutRequestBody;
import org.waterwood.waterfunservicecore.api.req.auth.PwdLoginReq;
import org.waterwood.waterfunservicecore.api.resp.auth.LoginClientData;
import org.waterwood.waterfunservicecore.infrastructure.aspect.RateLimit;
import org.waterwood.waterfunservicecore.infrastructure.utils.CookieUtil;
import org.waterwood.waterfunservicecore.infrastructure.utils.ResponseUtil;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.auth.AuthCoreService;
import org.waterwood.waterfunservicecore.services.auth.CaptchaService;
import org.waterwood.waterfunservicecore.services.auth.LineCaptchaResult;
import org.waterwood.waterfunservicecore.services.auth.LoginService;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;
import org.waterwood.waterfunservicecore.services.user.UserProfileCoreService;

import java.io.IOException;

@RestController
@RequestMapping("/api/admin/auth")
public class AuthController {

    private final CaptchaService captchaService;
    private final LoginService loginService;
    private final AuthCoreService authCoreService;
    private final AuthService authService;

    public AuthController(CaptchaService captchaService, LoginService loginService, AuthCoreService authCoreService, UserCoreService userCoreService, AuthService authService, UserService userService, UserProfileCoreService userProfileCoreService) {
        this.captchaService = captchaService;
        this.loginService = loginService;
        this.authCoreService = authCoreService;
        this.authService = authService;
    }

    @RateLimit(key = "ip", permits = 10, window = 60)
    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public void getCaptcha(HttpServletResponse response) throws IOException {
        LineCaptchaResult result = captchaService.generateCaptcha();
        Cookie cookie = new Cookie("ADMIN_CAPTCHA_KEY",result.uuid());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(120);
        response.addCookie(cookie);
        // set the header ofPending response
        response.setContentType("image/jpeg");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setDateHeader("Expires", 0);
        // write img stream to response stream
        result.captcha().write(response.getOutputStream());
    }

    @RateLimit(key = "ip", permits = 3, window = 60)
    @Operation(summary = "管理员密码登陆")
    @PostMapping("/login-by-password")
    public ApiResponse<LoginClientData> loginByPassword(@Valid @RequestBody PwdLoginReq body, HttpServletRequest request, HttpServletResponse response) {
        return ApiResponse.success(
                authService.loginByPwd(body, request, response)
        );
    }

    @RateLimit(key = "admin.password.change", permits = 3, window = 60)
    @Operation(summary = "管理员修改密码")
    @PostMapping("/password/change")
    public ApiResponse<Void> changePassword(@RequestBody @Valid AdminChangePasswordReq body) {
        long uid = UserCtxHolder.getUserUid();
        authService.adminChangePassword(uid, body);
        return ApiResponse.success();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody @Valid LogoutRequestBody req, HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = CookieUtil.getCookieValue(request.getCookies(),"REFRESH_TOKEN");
            loginService.logout(refreshToken, req.getDeviceFp());
            return ApiResponse.success();
        } finally {
            CookieUtil.cleanTokenCookie(response);
        }
    }

    @Operation(summary = "刷新 Token")
    @PostMapping("/refresh")
    public ApiResponse<LoginClientData> refresh(String deviceFp, HttpServletRequest request, HttpServletResponse response) {
        TokenPair tokenPair = authCoreService.refreshAccessToken(
                CookieUtil.getCookieValue(request.getCookies(), "REFRESH_TOKEN"),
                deviceFp
        );
        CookieUtil.setTokenCookie(response, tokenPair);
        ResponseUtil.setNoCacheSecurityHeaders(response);
        return ApiResponse.success(new LoginClientData(
                tokenPair.accessToken(), tokenPair.accessExp(), false
        ));
    }
}
