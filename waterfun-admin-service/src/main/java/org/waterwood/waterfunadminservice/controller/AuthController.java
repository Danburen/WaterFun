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
import org.waterwood.waterfunservicecore.api.req.auth.PwdLoginReq;
import org.waterwood.waterfunservicecore.api.req.user.UpdateUserProfileRequest;
import org.waterwood.waterfunservicecore.api.resp.auth.LoginClientData;
import org.waterwood.waterfunservicecore.entity.user.User;
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
    private final AuthCoreService authCoreService;
    private final UserCoreService userCoreService;
    private final AuthService authService;
    private final UserService userService;
    private final UserProfileCoreService userProfileCoreService;

    public AuthController(CaptchaService captchaService, LoginService loginService, AuthCoreService authCoreService, UserCoreService userCoreService, AuthService authService, UserService userService, UserProfileCoreService userProfileCoreService) {
        this.captchaService = captchaService;
        this.loginService = loginService;
        this.authCoreService = authCoreService;
        this.userCoreService = userCoreService;
        this.authService = authService;
        this.userService = userService;
        this.userProfileCoreService = userProfileCoreService;
    }

    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public void getCaptcha(HttpServletResponse response) throws IOException {
        LineCaptchaResult result = captchaService.generateCaptcha();
        Cookie cookie = new Cookie("ADMIN_CAPTCHA_KEY",result.uuid());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(120);
        response.addCookie(cookie);
        // set the header of response
        response.setContentType("image/jpeg");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setDateHeader("Expires", 0);
        // write img stream to response stream
        result.captcha().write(response.getOutputStream());
    }

    @Operation(summary = "管理员密码登陆")
    @PostMapping("/login-by-password")
    public ApiResponse<LoginClientData> loginByPassword(@Valid @RequestBody PwdLoginReq body, HttpServletRequest request, HttpServletResponse response) {
        return ApiResponse.success(
                authService.loginByPwd(body, request, response)
        );
    }

    @PostMapping("/info")
    public ApiResponse<AdminUserInfoResponse> getCurrentUserInfo(){
        return ApiResponse.success(
            userService.getCurrentAdminUserInfo()
        );
    }

    @PutMapping("/updateProfile")
    public ApiResponse<Void> updateProfile(@RequestBody @Valid UpdateUserProfileRequest body){
        userProfileCoreService.updateProfileByDto(body);
        return ApiResponse.success();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody String deviceFp, HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = CookieUtil.getCookieValue(request.getCookies(),"REFRESH_TOKEN");
        boolean  result = loginService.logout(refreshToken, deviceFp);
        if(result) CookieUtil.cleanTokenCookie(response);
        return ApiResponse.success();
    }
}
