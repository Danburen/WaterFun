package org.waterwood.waterfunservice.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.common.TokenResult;
import org.waterwood.utils.MaskUtil;
import org.waterwood.waterfunservice.api.request.ChangeEmailReq;
import org.waterwood.waterfunservice.api.request.ChangePhoneReq;
import org.waterwood.waterfunservice.api.request.EmailChangeVerifyReq;
import org.waterwood.waterfunservice.api.request.EmailReAuthReq;
import org.waterwood.waterfunservice.api.request.PasswordChangeReq;
import org.waterwood.waterfunservice.api.request.PhoneChangeVerifyReq;
import org.waterwood.waterfunservice.api.request.PhoneReAuthReq;
import org.waterwood.waterfunservice.api.request.ReAuthReq;
import org.waterwood.waterfunservice.api.request.ReAuthVerifyReq;
import org.waterwood.waterfunservice.api.response.EmailChangeVo;
import org.waterwood.waterfunservice.api.response.PhoneChangeVo;
import org.waterwood.waterfunservice.api.response.ReAuthInfoResp;
import org.waterwood.waterfunservice.api.response.ReAuthTokenVo;
import org.waterwood.waterfunservicecore.services.account.AccountCoreService;
import org.waterwood.waterfunservicecore.api.auth.VerifyChannel;
import org.waterwood.waterfunservicecore.api.auth.VerifyScene;
import org.waterwood.waterfunservicecore.api.resp.AccountResp;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;
import org.waterwood.waterfunservicecore.infrastructure.aspect.RateLimit;
import org.waterwood.waterfunservicecore.infrastructure.utils.CookieUtil;
import org.waterwood.waterfunservicecore.infrastructure.utils.ResponseUtil;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.auth.code.VerificationService;
import org.waterwood.waterfunservicecore.services.auth.SingleUseTokenService;
import org.waterwood.waterfunservicecore.services.user.UserDatumCoreService;

@Slf4j
@RestController
@RequestMapping("/api/auth/account")
@RequiredArgsConstructor
public class AuthAccountController {
    private final AccountCoreService accountCoreService;
    private final VerificationService verificationService;
    private final UserDatumCoreService userDatumCoreService;
    private final SingleUseTokenService singleUseTokenService;

    @RateLimit(key = "account.default", permits = 10, window = 300)
    @GetMapping
    public ApiResponse<AccountResp> get(){
        return ApiResponse.success(
            userDatumCoreService.getAccountInfo(UserCtxHolder.getUserUid())
        );
    }

    @Operation(summary = "获取 re-auth 信息（掩码手机号）")
    @GetMapping("/re-auth/info")
    public ApiResponse<ReAuthInfoResp> getReAuthInfo() {
        long uid = UserCtxHolder.getUserUid();
        String rawPhone = userDatumCoreService.getRawPhone(uid);
        if (rawPhone == null) {
            return ApiResponse.success(new ReAuthInfoResp(null));
        }
        String masked = MaskUtil.maskPhone(rawPhone);
        return ApiResponse.success(new ReAuthInfoResp(masked));
    }

    @Operation(summary = "发起 re-auth（直接发验证码到绑定手机）")
    @PostMapping("/re-auth")
    @RateLimit(key = "ip", permits = 3, window = 60)
    public ApiResponse<Void> reAuth(@Valid @RequestBody ReAuthReq body,
                                     HttpServletResponse response) {
        long uid = UserCtxHolder.getUserUid();
        String phone = userDatumCoreService.getRawPhone(uid);
        if (phone == null) {
            return ApiResponse.success();
        }
        CodeResult result = verificationService.sendCodeForAuthenticated(phone, VerifyChannel.SMS, body.getScene());
        ResponseUtil.setCookieAndNoCache(response, body.getScene().name() + "_REAUTH_KEY", result.getKey(), 300);
        return ApiResponse.success();
    }

    @Operation(summary = "验证 re-auth 验证码，获取 reAuthToken")
    @PostMapping("/re-auth/verify")
    @RateLimit(key = "ip", permits = 5, window = 300)
    public ApiResponse<ReAuthTokenVo> verifyReAuth(@Valid @RequestBody ReAuthVerifyReq body,
                                                   HttpServletRequest request) {
        long uid = UserCtxHolder.getUserUid();
        String phone = userDatumCoreService.getRawPhone(uid);
        if (phone == null) {
            return ApiResponse.success(new ReAuthTokenVo(null));
        }
        String verifyKey = CookieUtil.getCookieValue(request.getCookies(), body.getScene().name() + "_REAUTH_KEY");
        if (verifyKey == null) {
            return ApiResponse.success(new ReAuthTokenVo(null));
        }
        verificationService.verifyCode(phone, body.getScene(), VerifyChannel.SMS, verifyKey, body.getCode());
        TokenResult token = singleUseTokenService.generateVerifyToken(body.getScene().name(), uid);
        return ApiResponse.success(new ReAuthTokenVo(token));
    }

    @Operation(summary = "重置密码")
    @PostMapping("/password/reset")
    @RateLimit(key = "account.default", permits = 5, window = 300)
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordChangeReq body) {
        Long uid = singleUseTokenService.consumeVerifyToken(body.getReAuthToken(), VerifyScene.RESET_PASSWORD.name());
        accountCoreService.changePwd(uid, body.getNewPwd(), body.getConfirmPwd());
        return ApiResponse.success();
    }

    @Operation(summary = "设置密码")
    @PostMapping("/password/set")
    @RateLimit(key = "account.default", permits = 5, window = 300)
    public ApiResponse<Void> setPassword(@Valid @RequestBody PasswordChangeReq body) {
        Long uid = singleUseTokenService.consumeVerifyToken(body.getReAuthToken(), VerifyScene.SET_PASSWORD.name());
        accountCoreService.setPassword(uid, body.getNewPwd(), body.getConfirmPwd());
        return ApiResponse.success();
    }

    @Operation(summary = "激活邮箱")
    @PostMapping("/email/activate")
    @RateLimit(key = "account.default", permits = 5, window = 300)
    public ApiResponse<Void> activateEmail(@Valid @RequestBody EmailReAuthReq body) {
        Long uid = singleUseTokenService.consumeVerifyToken(body.getReAuthToken(), VerifyScene.ACTIVATE.name());
        accountCoreService.activateEmail(uid, body.getEmail());
        return ApiResponse.success();
    }

    @Operation(summary = "修改邮箱 — 保存为新邮箱（未激活）并发送激活码")
    @PostMapping("/email/change")
    @RateLimit(key = "account.default", permits = 5, window = 300)
    public ApiResponse<EmailChangeVo> changeEmail(@Valid @RequestBody ChangeEmailReq body) {
        Long uid = singleUseTokenService.consumeVerifyToken(body.getReAuthToken(), VerifyScene.CHANGE_EMAIL.name());
        CodeResult result = accountCoreService.changeEmail(uid, body.getNewEmail());
        return ApiResponse.success(new EmailChangeVo(result.getKey()));
    }

    @Operation(summary = "验证新邮箱激活码，完成换邮箱")
    @PostMapping("/email/change/verify")
    @RateLimit(key = "account.default", permits = 5, window = 300)
    public ApiResponse<Void> verifyChangeEmail(@Valid @RequestBody EmailChangeVerifyReq body) {
        long uid = UserCtxHolder.getUserUid();
        accountCoreService.verifyChangeEmail(body.getVerifyKey(), body.getCode(), uid);
        return ApiResponse.success();
    }

    @Operation(summary = "发起修改手机号 — 保存为新手机号（未激活）并发送激活码")
    @PostMapping("/phone/change")
    @RateLimit(key = "account.default", permits = 5, window = 300)
    public ApiResponse<PhoneChangeVo> changePhone(@Valid @RequestBody ChangePhoneReq body) {
        Long uid = singleUseTokenService.consumeVerifyToken(body.getReAuthToken(), VerifyScene.CHANGE_PHONE.name());
        CodeResult result = accountCoreService.changePhone(uid, body.getNewPhone());
        return ApiResponse.success(new PhoneChangeVo(result.getKey()));
    }

    @Operation(summary = "验证新手机号激活码，完成换手机号")
    @PostMapping("/phone/change/verify")
    @RateLimit(key = "account.default", permits = 5, window = 300)
    public ApiResponse<Void> verifyChangePhone(@Valid @RequestBody PhoneChangeVerifyReq body) {
        long uid = UserCtxHolder.getUserUid();
        accountCoreService.verifyChangePhone(body.getVerifyKey(), body.getCode(), uid);
        return ApiResponse.success();
    }

    @Operation(summary = "激活手机号")
    @PostMapping("/phone/activate")
    @RateLimit(key = "account.default", permits = 5, window = 300)
    public ApiResponse<Void> activatePhone(@Valid @RequestBody PhoneReAuthReq body) {
        Long uid = singleUseTokenService.consumeVerifyToken(body.getReAuthToken(), VerifyScene.ACTIVATE.name());
        accountCoreService.activatePhone(uid, body.getPhone());
        return ApiResponse.success();
    }

    @Operation(summary = "解绑邮箱")
    @PostMapping("/email/unbind")
    @RateLimit(key = "account.default", permits = 5, window = 300)
    public ApiResponse<Void> unbindEmail(@Valid @RequestBody EmailReAuthReq body) {
        Long uid = singleUseTokenService.consumeVerifyToken(body.getReAuthToken(), VerifyScene.UNBIND.name());
        accountCoreService.unbindEmail(uid, body.getEmail());
        return ApiResponse.success();
    }
}
