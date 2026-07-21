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
import org.waterwood.waterfunservicecore.api.req.auth.LogoutRequestBody;
import org.waterwood.waterfunservicecore.infrastructure.utils.CookieUtil;
import org.waterwood.waterfunservicecore.services.auth.LoginService;

@Slf4j
@RestController
@RequestMapping("/api/user/security")
@RequiredArgsConstructor
public class UserSecurityController {
    private final LoginService loginService;

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
