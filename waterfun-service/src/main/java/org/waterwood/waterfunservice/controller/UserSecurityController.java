package org.waterwood.waterfunservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservice.service.account.AccountService;
import org.waterwood.waterfunservicecore.api.req.auth.SecuritySendCodeDto;
import org.waterwood.waterfunservicecore.infrastructure.aspect.RateLimit;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;
import org.waterwood.waterfunservicecore.infrastructure.utils.CookieUtil;
import org.waterwood.waterfunservicecore.infrastructure.utils.ResponseUtil;
import org.waterwood.waterfunservicecore.api.req.auth.LogoutRequestBody;
import org.waterwood.waterfunservicecore.services.auth.AuthCoreService;
import org.waterwood.waterfunservicecore.services.auth.LoginService;
import org.waterwood.waterfunservicecore.services.auth.code.VerificationService;

@Slf4j
@RestController
@RequestMapping("/api/user/security")
@RequiredArgsConstructor
public class UserSecurityController {
    private final VerificationService verificationService;
    private final LoginService loginService;
    private final AccountService accountService;
    private final AuthCoreService authCoreService;

    @Operation(summary = "发送验证后验证码")
    @PostMapping("/send-verify-code")
    @RateLimit(key = "ip", permits = 5, window = 300)
    public ApiResponse<Void> sendVerifyCode(@Valid @RequestBody SecuritySendCodeDto dto, HttpServletResponse response) {
        CodeResult result = verificationService.sendAutoTargetAuthenticationCode(
                dto.getChannel(),
                dto.getScene());
        String cookieKey = dto.getChannel().name() + "_CODE_KEY";
        ResponseUtil.setCookieAndNoCache(response,cookieKey, result.getKey(), 120);
        return ApiResponse.success();
    }

    @Operation(summary = "登出")
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
}
