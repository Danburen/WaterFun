package org.waterwood.waterfunservice.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.api.AuthCode;
import org.waterwood.api.TokenPair;
import org.waterwood.common.exceptions.AuthException;
import org.waterwood.waterfunservice.service.log.AuditLogService;
import org.waterwood.waterfunservicecore.api.auth.LoginResult;
import org.waterwood.waterfunservicecore.api.auth.VerifyChannel;
import org.waterwood.waterfunservicecore.api.req.auth.PwdLoginReq;
import org.waterwood.waterfunservicecore.api.req.auth.RegisterRequest;
import org.waterwood.waterfunservicecore.api.req.auth.SendCodeDto;
import org.waterwood.waterfunservicecore.api.req.auth.VerifyCodeDto;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;
import org.waterwood.waterfunservicecore.api.resp.auth.LoginClientData;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogActionType;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogStatusType;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.infrastructure.aspect.RateLimit;
import org.waterwood.waterfunservicecore.infrastructure.utils.CookieUtil;
import org.waterwood.waterfunservicecore.infrastructure.utils.ResponseUtil;
import org.waterwood.waterfunservicecore.services.auth.LineCaptchaResult;
import org.waterwood.waterfunservicecore.services.auth.code.VerificationService;
import org.waterwood.waterfunservicecore.services.auth.impl.AuthCoreServiceImpl;
import org.waterwood.waterfunservicecore.services.auth.impl.CaptchaServiceImpl;
import org.waterwood.waterfunservicecore.services.auth.impl.LoginServiceImpl;
import org.waterwood.waterfunservicecore.services.auth.impl.RegisterServiceImpl;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@RestController
@Validated
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final CaptchaServiceImpl captchaService;
    private final LoginServiceImpl loginService;
    private final RegisterServiceImpl registerService;
    private final AuthCoreServiceImpl authService;
    private final VerificationService verificationService;
    private final AuditLogService auditLogService;


    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    @RateLimit(key = "auth.captcha", permits = 30, window = 60)
    public void getCaptcha(HttpServletResponse response) throws IOException{
        LineCaptchaResult result = captchaService.generateCaptcha();
        Cookie cookie = new Cookie("CAPTCHA_KEY",result.uuid());
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

    @GetMapping("/csrf-token")
    public ApiResponse<Void> getCsrfToken(HttpServletRequest request,HttpServletResponse response) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if(token == null){
            return ApiResponse.success();
        }
        ResponseCookie cookie = ResponseCookie.from("XSRF-TOKEN", token.getToken())
                .httpOnly(false)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofHours(1))
                .build();
        response.setHeader("Set-cookie", cookie.toString());
        return ApiResponse.success();
    }

    /***
     * Send sms/email code api for scene only allow login or register
     * @param dto send code request
     * @param response http response
     * @return send code result
     */
    @Operation(summary = "发送验证码（需图形验证码）")
    @PostMapping("/send-code")
    public ApiResponse<Void> sendCode(@Valid @RequestBody SendCodeDto dto,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        String captchaKey = CookieUtil.getCookieValue(request.getCookies(), "CAPTCHA_KEY");
        if (!captchaService.verifyCode(captchaKey, dto.getCaptcha())) {
            throw new AuthException(AuthCode.CAPTCHA_INVALID);
        }
        CodeResult result = verificationService.sendCode(dto);
        String cookieKey = dto.getChannel().name() + "_CODE_KEY";
        ResponseUtil.setCookieAndNoCache(response, cookieKey, result.getKey(), 120);
        return ApiResponse.success();
    }

    @Operation(summary = "密码登陆")
    @PostMapping("/login-by-password")
    @RateLimit(key = "auth.login.password", permits = 5, window = 300)
    public ApiResponse<LoginClientData> loginByPassword(@Valid @RequestBody PwdLoginReq body, HttpServletRequest request, HttpServletResponse response) {
        try {
            Cookie[] cookies = request.getCookies();
            LoginResult result = loginService.login(body, CookieUtil.getCookieValue(cookies, "CAPTCHA_KEY"));
            auditLogService.record(result.user().getUid(), result.user().getUsername(), AuditLogActionType.LOGIN,
                    request, body.getDeviceInfo());
            return ApiResponse.success(
                    authService.BuildLoginResponse(response, result.user(), body.getDeviceFp(), false)
            );
        } catch (Exception e) {
            auditLogService.record(null, body.getUsername(), AuditLogActionType.LOGIN,
                    AuditLogStatusType.FAIL, e.getMessage(), request, body.getDeviceInfo());
            throw e;
        }
    }


    @Operation(summary = "手机/邮箱登陆")
    @PostMapping("/login-by-code")
    @RateLimit(key = "auth.login.code", permits = 5, window = 300)
    public ApiResponse<LoginClientData> loginByCode(@Valid @RequestBody VerifyCodeDto dto, HttpServletRequest request, HttpServletResponse response) {
        try {
            String codeKey = dto.getChannel() == VerifyChannel.SMS ? "SMS_CODE_KEY" : "EMAIL_CODE_KEY";
            LoginResult result = loginService.login(dto, CookieUtil.getCookieValue(request, codeKey));
            auditLogService.record(result.user().getUid(), result.user().getUsername(), AuditLogActionType.LOGIN,
                    request, dto.getDeviceInfo());
            return ApiResponse.success(
                    authService.BuildLoginResponse(response, result.user(), dto.getDeviceFp(), result.isNewUser())
            );
        } catch (Exception e) {
            auditLogService.record(null, dto.getTarget(), AuditLogActionType.LOGIN,
                    AuditLogStatusType.FAIL, e.getMessage(), request, dto.getDeviceInfo());
            throw e;
        }
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    @RateLimit(key = "auth.register", permits = 3, window = 300)
    public ApiResponse<LoginClientData> register(@Valid @RequestBody RegisterRequest dto, HttpServletRequest request, HttpServletResponse response) {
        try {
            User user = registerService.register(
                    dto,
                    CookieUtil.getCookieValue(request.getCookies(), "SMS_CODE_KEY")
            );
            auditLogService.record(user.getUid(), user.getUsername(), AuditLogActionType.REGISTER,
                    request, dto.getVerify().getDeviceInfo());
            return ApiResponse.success(
                    authService.BuildLoginResponse(response, user, dto.getVerify().getDeviceFp(), true)
            );
        } catch (Exception e) {
            auditLogService.record(null, dto.getUsername(), AuditLogActionType.REGISTER,
                    AuditLogStatusType.FAIL, e.getMessage(), request, dto.getVerify().getDeviceInfo());
            throw e;
        }
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginClientData> refresh(@Valid @NotNull String deviceFp, HttpServletRequest request, HttpServletResponse response) {
        TokenPair tokenPair = authService.refreshAccessToken(
                CookieUtil.getCookieValue(request.getCookies(),"REFRESH_TOKEN"),
                deviceFp
        );
        CookieUtil.setTokenCookie(response, tokenPair);
        ResponseUtil.setNoCacheSecurityHeaders(response);
        return ApiResponse.success(new LoginClientData(
                tokenPair.accessToken(), tokenPair.accessExp(), false
        ));
    }
}
