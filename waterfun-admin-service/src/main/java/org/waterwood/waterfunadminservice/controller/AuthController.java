package org.waterwood.waterfunadminservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunadminservice.api.response.user.AdminUserInfoResponse;
import org.waterwood.waterfunadminservice.service.auth.AuthService;
import org.waterwood.waterfunadminservice.service.auth.UserService;
import org.waterwood.waterfunservicecore.api.req.auth.LogoutRequestBody;
import org.waterwood.waterfunservicecore.api.req.auth.PwdLoginReq;
import org.waterwood.waterfunservicecore.api.req.user.UpdateUserProfileRequest;
import org.waterwood.waterfunservicecore.api.resp.auth.LoginClientData;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.infrastructure.aspect.RateLimit;
import org.waterwood.waterfunservicecore.infrastructure.utils.CookieUtil;
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
    private final AuthService authService;

    public AuthController(CaptchaService captchaService, LoginService loginService, AuthCoreService authCoreService, UserCoreService userCoreService, AuthService authService, UserService userService, UserProfileCoreService userProfileCoreService) {
        this.captchaService = captchaService;
        this.loginService = loginService;
        this.authService = authService;
    }

    @Operation(summary = "获取 CSRF Token")
    @GetMapping("/csrf-token")
    public ApiResponse<Void> csrfToken(HttpServletRequest request) {
        // CSRF 启用时：CsrfFilter + CookieCsrfTokenRepository 自动设置 XSRF-TOKEN cookie
        // CSRF 关闭时：仅返回 success，前端收到 200 即不再报错
        return ApiResponse.success();
    }

    @RateLimit(key = "auth.login.captcha", permits = 5)
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

    @RateLimit(key = "admin.login.in", permits = 3, window = 60)
    @Operation(summary = "管理员密码登陆")
    @PostMapping("/login-by-password")
    public ApiResponse<LoginClientData> loginByPassword(@Valid @RequestBody PwdLoginReq body, HttpServletRequest request, HttpServletResponse response) {
        return ApiResponse.success(
                authService.loginByPwd(body, request, response)
        );
    }
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody @Valid LogoutRequestBody req, HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = CookieUtil.getCookieValue(request.getCookies(),"REFRESH_TOKEN");
        loginService.logout(refreshToken, req.getDeviceFp());
        CookieUtil.cleanTokenCookie(response);
        return ApiResponse.success();
    }
}
