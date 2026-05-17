package org.waterwood.waterfunservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.waterwood.api.ApiResponse;
import org.waterwood.common.TokenResult;
import org.waterwood.waterfunservice.service.account.AccountService;
import org.waterwood.waterfunservicecore.api.req.auth.SecuritySendCodeDto;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;
import org.waterwood.waterfunservicecore.api.resp.auth.LoginClientData;
import org.waterwood.waterfunservicecore.infrastructure.utils.CookieUtil;
import org.waterwood.waterfunservicecore.infrastructure.utils.ResponseUtil;
import org.waterwood.waterfunservicecore.services.auth.AuthCoreService;
import org.waterwood.waterfunservicecore.services.auth.LoginService;
import org.waterwood.waterfunservicecore.services.auth.code.VerificationService;

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
    public ApiResponse<Void> logout(@RequestBody String deviceFp, HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = CookieUtil.getCookieValue(request.getCookies(),"REFRESH_TOKEN");
        boolean  result = loginService.logout(refreshToken, deviceFp);
        if(result) CookieUtil.cleanTokenCookie(response);
        return ApiResponse.success();
    }

    @Operation(summary = "刷新access token")
    @PostMapping("/refresh-access-token")
    public ApiResponse<LoginClientData> refreshAccessToken(@Valid @NotBlank(message = "{auth.device_fingerprint.required}") String dfp, HttpServletRequest request) {
        TokenResult res = authCoreService.refreshAccessToken(
                CookieUtil.getCookieValue(request.getCookies(),"REFRESH_TOKEN"),
                dfp
        );
        LoginClientData data = new LoginClientData(res.tokenValue(),res.expire());
        return ApiResponse.success(data);
    }
}
