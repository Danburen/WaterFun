package org.waterwood.waterfunservice.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservice.api.request.PhoneChangeActivateDto;
import org.waterwood.waterfunservicecore.api.auth.VerifyChannel;
import org.waterwood.waterfunservicecore.api.req.auth.SendCodeDto;
import org.waterwood.waterfunservicecore.infrastructure.aspect.RateLimit;
import org.waterwood.waterfunservicecore.api.resp.AccountResp;
import org.waterwood.waterfunservice.api.request.EmailChangeDto;
import org.waterwood.waterfunservice.api.request.ResetPasswordDto;
import org.waterwood.waterfunservice.api.request.SetPasswordDto;
import org.waterwood.waterfunservice.api.request.EmailBindActivateDto;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;
import org.waterwood.waterfunservice.service.log.AuditLogService;
import org.waterwood.waterfunservicecore.infrastructure.utils.ResponseUtil;
import org.waterwood.waterfunservice.service.account.AccountService;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogActionType;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogStatusType;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.auth.code.VerificationService;
import org.waterwood.waterfunservicecore.services.user.UserDatumCoreService;
import org.waterwood.waterfunservicecore.infrastructure.utils.CookieKeyGetter;

@Slf4j
@RestController
@RequestMapping("/api/auth/account")
@RequiredArgsConstructor
public class AuthAccountController {
    private final AccountService accountService;
    private final VerificationService verificationService;
    private final UserDatumCoreService userDatumCoreService;
    private final AuditLogService auditLogService;

    @RateLimit(key = "account.default", permits = 5, window = 300)
    @GetMapping
    public ApiResponse<AccountResp> get(){
        return ApiResponse.success(
            userDatumCoreService.getAccountInfo(UserCtxHolder.getUserUid())
        );
    }

    @Operation(summary = "账户页发送验证码")
    @PostMapping("/send-verify-code")
    @RateLimit(key = "account.default", permits = 5, window = 300)
    public ApiResponse<Void> sendVerifyCode(@Valid @RequestBody SendCodeDto dto, HttpServletResponse response) {
        CodeResult result = verificationService.sendCode(dto);
        String cookieKey = dto.getChannel().name() + "_CODE_KEY";
        ResponseUtil.setCookieAndNoCache(response, cookieKey, result.getKey(), 120);
        return ApiResponse.success();
    }

    @Operation(summary = "重置密码")
    @PostMapping("/password/reset")
    @RateLimit(key = "account.default", permits = 5, window = 300)
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordDto dto, HttpServletRequest request) {
        long userUid = UserCtxHolder.getUserUid();
        try {
            accountService.changePwd(
                    CookieKeyGetter.getChannelVerifyCodeKey(dto.getVerify().getChannel(), request.getCookies()),
                    dto);
            auditLogService.record(userUid, null, AuditLogActionType.CHANGE_PASSWORD,
                    request, dto.getVerify().getDeviceInfo());
            return ApiResponse.success();
        } catch (Exception e) {
            auditLogService.record(userUid, null, AuditLogActionType.CHANGE_PASSWORD,
                    AuditLogStatusType.FAIL, e.getMessage(), request, dto.getVerify().getDeviceInfo());
            throw e;
        }
    }

    @Operation(summary = "设置密码")
    @PostMapping("/password/set")
    @RateLimit(key = "account.default", permits = 5, window = 300)
    public ApiResponse<Void> setPassword(@Valid @RequestBody SetPasswordDto dto, HttpServletRequest request) {
        long userUid = UserCtxHolder.getUserUid();
        try {
            accountService.setPassword(
                    CookieKeyGetter.getChannelVerifyCodeKey(dto.getVerify().getChannel(), request.getCookies()),
                    dto);
            auditLogService.record(userUid, null, AuditLogActionType.CHANGE_PASSWORD,
                    request, dto.getVerify().getDeviceInfo());
            return ApiResponse.success();
        } catch (Exception e) {
            auditLogService.record(userUid, null, AuditLogActionType.CHANGE_PASSWORD,
                    AuditLogStatusType.FAIL, e.getMessage(), request, dto.getVerify().getDeviceInfo());
            throw e;
        }
    }
    @Operation(summary = "绑定邮箱")
    @PostMapping("/email/bind")
    @RateLimit(key = "account.default", permits = 5, window = 300)
    public ApiResponse<Void> bindEmail(@Valid @RequestBody EmailBindActivateDto dto, HttpServletRequest req, HttpServletResponse res) {
        CodeResult result =  accountService.bindEmail(
                CookieKeyGetter.getChannelVerifyCodeKey(dto.getVerify().getChannel(), req.getCookies()),
                dto
        );
        String cookieKey = "EMAIL_CODE_KEY";
        ResponseUtil.setCookieAndNoCache(res, cookieKey, result.getKey(), 120);
        return ApiResponse.success();
    }
    @Operation(summary = "激活邮箱")
    @PostMapping("/email/activate")
    @RateLimit(key = "account.default", permits = 5, window = 300)
    public ApiResponse<Void> activateEmail(@Valid @RequestBody EmailBindActivateDto dto, HttpServletRequest req){
        accountService.activateEmail(
                CookieKeyGetter.getChannelVerifyCodeKey(dto.getVerify().getChannel(), req.getCookies()),
                dto
        );
        return ApiResponse.success();
    }


    @Operation(summary = "修改邮箱")
    @PostMapping("/email/change")
    @RateLimit(key = "account.default", permits = 5, window = 300)
    public ApiResponse<Void> changeEmail(@Valid @RequestBody EmailChangeDto dto, HttpServletRequest req, HttpServletResponse res) {
        VerifyChannel channel = dto.getVerify().getChannel();
        CodeResult result = accountService.changeEmail(
                CookieKeyGetter.getChannelVerifyCodeKey(channel, req.getCookies()),
                dto
        );
        String cookieKey = channel.name() + "_CODE_KEY";
        ResponseUtil.setCookieAndNoCache(res, cookieKey, result.getKey(), 120);
        return ApiResponse.success();
    }

    @Operation(summary = "修改手机号")
    @PostMapping("/phone/change")
    @RateLimit(key = "account.default", permits = 5, window = 300)
    public ApiResponse<Void> changePhone(@Valid @RequestBody PhoneChangeActivateDto dto, HttpServletRequest req, HttpServletResponse res) {
        CodeResult result = accountService.changePhone(
                CookieKeyGetter.getChannelVerifyCodeKey(dto.getVerify().getChannel(), req.getCookies()),
                dto
        );
        String cookieKey = "SMS_CODE_KEY";
        ResponseUtil.setCookieAndNoCache(res, cookieKey, result.getKey(), 120);
        return ApiResponse.success();
    }

    @Operation(summary = "激活手机号")
    @PostMapping("/phone/activate")
    @RateLimit(key = "account.default", permits = 5, window = 300)
    public ApiResponse<Void> activatePhone(@Valid @RequestBody PhoneChangeActivateDto dto, HttpServletRequest req) {
        accountService.activatePhone(
                CookieKeyGetter.getChannelVerifyCodeKey(dto.getVerify().getChannel(), req.getCookies()),
                dto
        );
        return ApiResponse.success();
    }

    @Operation(summary = "解绑邮箱")
    @PostMapping("/email/unbind")
    @RateLimit(key = "account.default", permits = 5, window = 300)
    public ApiResponse<Void> unbindEmail(@Valid @RequestBody EmailBindActivateDto dto, HttpServletRequest req,
                                         HttpServletResponse res) {
        accountService.unbindEmail(
                CookieKeyGetter.getChannelVerifyCodeKey(dto.getVerify().getChannel(), req.getCookies()),
                dto
        );
        return ApiResponse.success();
    }
}
