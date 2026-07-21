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
import org.waterwood.common.TokenResult;
import org.waterwood.waterfunservice.api.request.ForgotPasswordReAuthReq;
import org.waterwood.waterfunservice.api.request.ForgotPasswordResetReq;
import org.waterwood.waterfunservice.api.request.ForgotPasswordVerifyReq;
import org.waterwood.waterfunservice.api.response.ReAuthKeyVo;
import org.waterwood.waterfunservice.api.response.ReAuthTokenVo;
import org.waterwood.common.exceptions.AuthException;
import org.waterwood.waterfunservicecore.api.auth.LoginResult;
import org.waterwood.waterfunservicecore.api.auth.VerifyChannel;
import org.waterwood.waterfunservicecore.api.auth.VerifyScene;
import org.waterwood.waterfunservicecore.api.req.auth.PwdLoginReq;
import org.waterwood.waterfunservicecore.api.req.auth.RegisterRequest;
import org.waterwood.waterfunservicecore.api.req.auth.SendCodeReq;
import org.waterwood.waterfunservicecore.api.req.auth.VerifyCodeDto;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;
import org.waterwood.waterfunservicecore.api.resp.auth.LoginClientData;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.infrastructure.aspect.RateLimit;
import org.waterwood.waterfunservicecore.infrastructure.utils.CookieUtil;
import org.waterwood.waterfunservicecore.infrastructure.utils.ResponseUtil;
import org.waterwood.waterfunservicecore.services.auth.LineCaptchaResult;
import org.waterwood.waterfunservicecore.services.auth.SingleUseTokenService;
import org.waterwood.waterfunservicecore.services.account.AccountCoreService;
import org.waterwood.waterfunservicecore.services.auth.code.VerificationService;
import org.waterwood.waterfunservicecore.services.auth.impl.*;

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
    private final SingleUseTokenService singleUseTokenService;
    private final AccountCoreService accountCoreService;


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

    @Deprecated
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
    public ApiResponse<Void> sendCode(@Valid @RequestBody SendCodeReq dto,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        String captchaKey = CookieUtil.getCookieValue(request.getCookies(), "CAPTCHA_KEY");
        if (!captchaService.verifyCode(captchaKey, dto.getCaptcha())) {
            throw new AuthException(AuthCode.CAPTCHA_INVALID);
        }
        CodeResult result = verificationService.sendCodeForAnonymous(dto);
        String cookieKey = dto.getChannel().name() + "_CODE_KEY";
        ResponseUtil.setCookieAndNoCache(response, cookieKey, result.getKey(), 120);
        return ApiResponse.success();
    }

    @Operation(summary = "密码登陆")
    @PostMapping("/login-by-password")
    @RateLimit(key = "ip", permits = 5, window = 300)
    public ApiResponse<LoginClientData> loginByPassword(@Valid @RequestBody PwdLoginReq body, HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        LoginResult result = loginService.login(body, CookieUtil.getCookieValue(cookies, "CAPTCHA_KEY"));
        return ApiResponse.success(
                authService.BuildLoginResponse(response, result.user(), body.getDeviceFp(), false)
        );
    }


    @Operation(summary = "手机/邮箱登陆")
    @PostMapping("/login-by-code")
    @RateLimit(key = "ip", permits = 5, window = 300)
    public ApiResponse<LoginClientData> loginByCode(@Valid @RequestBody VerifyCodeDto dto, HttpServletRequest request, HttpServletResponse response) {
        String codeKey = dto.getChannel() == VerifyChannel.SMS ? "SMS_CODE_KEY" : "EMAIL_CODE_KEY";
        LoginResult result = loginService.login(dto, CookieUtil.getCookieValue(request, codeKey));
        return ApiResponse.success(
                authService.BuildLoginResponse(response, result.user(), dto.getDeviceFp(), result.isNewUser())
        );
    }

    @Operation(summary = "忘记密码 - 发送验证码到绑定手机（通过任意标识符）")
    @PostMapping("/forgot-password/re-auth")
    @RateLimit(key = "ip", permits = 3, window = 60)
    public ApiResponse<ReAuthKeyVo> forgotPasswordReAuth(
            @Valid @RequestBody ForgotPasswordReAuthReq body,
            HttpServletRequest request,
            HttpServletResponse response) {
        String captchaKey = CookieUtil.getCookieValue(request.getCookies(), "CAPTCHA_KEY");
        if (!captchaService.verifyCode(captchaKey, body.getCaptcha())) {
            throw new AuthException(AuthCode.CAPTCHA_INVALID);
        }
        String reAuthKey = accountCoreService.initiateForgotPasswordReAuth(body.getIdentifier());
        if (reAuthKey != null) {
            ResponseUtil.setCookieAndNoCache(response, "SMS_CODE_KEY", reAuthKey, 300);
            return ApiResponse.success(new ReAuthKeyVo(reAuthKey));
        }
        return ApiResponse.success(new ReAuthKeyVo(null));
    }

    @Operation(summary = "忘记密码 - 验证码确认，获取 reAuthToken")
    @PostMapping("/forgot-password/re-auth/verify")
    @RateLimit(key = "ip", permits = 5, window = 300)
    public ApiResponse<ReAuthTokenVo> forgotPasswordVerifyReAuth(
            @Valid @RequestBody ForgotPasswordVerifyReq body) {
        TokenResult token = accountCoreService.verifyForgotPasswordReAuth(
                body.getReAuthKey(), body.getCode());
        return ApiResponse.success(new ReAuthTokenVo(token));
    }

    @Operation(summary = "忘记密码 - 使用 reAuthToken 重置密码")
    @PostMapping("/forgot-password/reset")
    @RateLimit(key = "ip", permits = 3, window = 300)
    public ApiResponse<Void> forgotPasswordReset(
            @Valid @RequestBody ForgotPasswordResetReq body) {
        Long uid = singleUseTokenService.consumeVerifyToken(
                body.getReAuthToken(), VerifyScene.FORGOT_PASSWORD.name());
        accountCoreService.resetPasswordByToken(uid, body.getNewPwd(), body.getConfirmPwd());
        return ApiResponse.success();
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    @RateLimit(key = "ip", permits = 3, window = 300)
    public ApiResponse<LoginClientData> register(@Valid @RequestBody RegisterRequest dto, HttpServletRequest request, HttpServletResponse response) {
        User user = registerService.register(
                dto,
                CookieUtil.getCookieValue(request.getCookies(), "SMS_CODE_KEY")
        );
        return ApiResponse.success(
                authService.BuildLoginResponse(response, user, dto.getVerify().getDeviceFp(), true)
        );
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
