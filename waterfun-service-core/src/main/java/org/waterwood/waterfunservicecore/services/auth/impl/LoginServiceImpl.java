package org.waterwood.waterfunservicecore.services.auth.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.AuthCode;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.utils.DateUtil;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.api.auth.LoginResult;
import org.waterwood.waterfunservicecore.api.auth.VerifyChannel;
import org.waterwood.waterfunservicecore.api.auth.VerifyScene;
import org.waterwood.waterfunservicecore.api.req.auth.DeviceInfoReq;
import org.waterwood.waterfunservicecore.api.req.auth.ForgetPasswordDto;
import org.waterwood.waterfunservicecore.api.req.auth.PwdLoginReq;
import org.waterwood.waterfunservicecore.api.req.auth.VerifyCodeDto;
import org.waterwood.waterfunservicecore.entity.EncryptionDataKey;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogActionType;
import org.waterwood.waterfunservicecore.entity.security.BanReasonType;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserDatum;
import org.waterwood.waterfunservicecore.entity.user.UserPermission;
import org.waterwood.common.exceptions.AuthException;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.waterfunservicecore.exception.ForbiddenException;
import org.waterwood.waterfunservicecore.exception.threshold.AttemptLimitExceededException;
import org.waterwood.waterfunservicecore.exception.threshold.DailyLimitExceededException;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelperHolder;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserDatumRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserPermRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRoleRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptedKeyService;
import org.waterwood.waterfunservicecore.infrastructure.security.RefreshTokenPayload;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.audit.AuditLogCoreService;
import org.waterwood.waterfunservicecore.services.auth.LoginService;
import org.waterwood.waterfunservicecore.services.auth.RegisterService;
import org.waterwood.waterfunservicecore.services.auth.code.VerificationService;
import org.waterwood.waterfunservicecore.services.online.OnlineUserService;
import org.waterwood.waterfunservicecore.services.stats.SiteStatisticRecorder;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;
import org.waterwood.utils.codec.HashUtil;

import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {
    private final UserRepository userRepo;
    private final TokenService tokenService;
    private final UserDatumRepo userDatumRepo;
    private final EncryptedKeyService encryptedKeyService;
    private final CaptchaServiceImpl captchaService;
    private final VerificationService verificationService;
    private final SiteStatisticRecorder siteStatisticRecorder;
    private final AuditLogCoreService auditLogCoreService;
    private final OnlineUserService onlineUserService;
    private final UserPermRepo userPermRepo;
    private final RegisterService registerService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UserRoleRepo userRoleRepo;
    private final RedisHelperHolder redisHelper;
    private final UserCoreService userCoreService;

    private static final int ADMIN_LOGIN_MAX_ATTEMPTS = 5;
    private static final long ADMIN_LOGIN_LOCK_MINUTES = 120;
    private static final int NORMAL_LOGIN_MAX_ATTEMPTS = 5;
    private static final long NORMAL_LOGIN_LOCK_MINUTES = 15;
    private static final long DAILY_NORMAL_LOGIN_MAX_ATTEMPTS = 30;
    private static final long DAILY_ADMIN_LOGIN_MAX_ATTEMPTS = 15;
    private static final String LOGIN_FAIL_PREFIX = "limit:temp:fail:login:";
    private static final String LOGIN_FAIL_DAILY_PREFIX = "limit:daily:fail:login:";

    @Override
    public void logout(String refreshToken, String dfp) {
        if(StringUtil.isBlank(refreshToken)) throw new AuthException(AuthCode.REAUTHORIZATION_REQUIRED);
        long userUid = UserCtxHolder.getUserUid();
        RefreshTokenPayload payload = tokenService.validateRefreshToken(userUid, refreshToken, dfp);
        tokenService.removeAccessToken(payload.userUid(), payload.deviceId());
        tokenService.removeRefreshToken(userUid, dfp, refreshToken);
    }

    @Override
    public LoginResult login(VerifyCodeDto dto, String codeKey) {
        DeviceInfoReq deviceInfo = dto.getDeviceInfo();
        try {
            if(dto.getScene() != VerifyScene.LOGIN){
                throw new AuthException(AuthCode.INVALID_VERIFY_SCENE);
            }
            VerifyChannel channel = dto.getChannel();

            EncryptionDataKey hmacKey = encryptedKeyService.getUserDatumHmacKey();
            UserDatum userDatum = null;
            if(channel == VerifyChannel.SMS){
                userDatum = userDatumRepo.findByPhoneHash(HashUtil.toSha256HmacString(dto.getTarget(), hmacKey.getEncryptedKey()))
                        .orElse(null);
            }else if(channel == VerifyChannel.EMAIL){
                userDatum = userDatumRepo.findByEmailHash(HashUtil.toSha256HmacString(dto.getTarget(), hmacKey.getEncryptedKey()))
                        .orElse(null);
            }

            if (userDatum == null) {
                User autoUser = registerService.autoRegister(
                        dto.getTarget(), channel, dto.getScene(), codeKey, dto.getCode(),
                        deviceInfo != null ? deviceInfo.getDeviceFp() : null
                );
                siteStatisticRecorder.recordLogin();
                checkLoginBan(autoUser.getUid());
                onlineUserService.updateLastActive(autoUser.getUid());
                auditLogCoreService.recordSuccess(autoUser.getUid(), autoUser.getUsername(),
                        AuditLogActionType.LOGIN, deviceInfo);
                return new LoginResult(autoUser, true);
            }
            User u = userDatum.getUser();
            verificationService.verifyCode(dto.getTarget(),dto.getScene(),channel,codeKey,dto.getCode());
            if(! isAdmin(u.getUid())){
                return recordAndBuildLoginResult(u, deviceInfo);
            } else {
                throw new ForbiddenException();
            }
        } catch (Exception e) {
            auditLogCoreService.recordFailure(null, dto.getTarget(), AuditLogActionType.LOGIN,
                    e.getMessage(), deviceInfo);
            throw e;
        }
    }

    @Override
    public LoginResult adminLogin(PwdLoginReq body, String verifyUuidKey) {
        DeviceInfoReq deviceInfo = body.getDeviceInfo();
        try {
            return tryLogin(body.getUsername(), true,
                    () -> verifyCredentials(body, verifyUuidKey), deviceInfo);
        } catch (Exception e) {
            auditLogCoreService.recordFailure(null, body.getUsername(), AuditLogActionType.LOGIN,
                    e.getMessage(), deviceInfo);
            throw e;
        }
    }

    @Override
    public LoginResult login(PwdLoginReq body, String verifyUuidKey) {
        DeviceInfoReq deviceInfo = body.getDeviceInfo();
        try {
            return tryLogin(body.getUsername(), false,
                    () -> verifyCredentials(body, verifyUuidKey), deviceInfo);
        } catch (Exception e) {
            auditLogCoreService.recordFailure(null, body.getUsername(), AuditLogActionType.LOGIN,
                    e.getMessage(), deviceInfo);
            throw e;
        }
    }

    @Override
    @Transactional
    public void forgetPassword(String verifyCodeKey, ForgetPasswordDto dto) {
        DeviceInfoReq deviceInfo = dto.getDeviceInfo();
        try {
            verificationService.verifyCode(
                    dto.getTarget(), VerifyScene.FORGOT_PASSWORD, dto.getChannel(),
                    verifyCodeKey, dto.getCode()
            );

            EncryptionDataKey hmacKey = encryptedKeyService.getUserDatumHmacKey();
            String channelHash = HashUtil.toSha256HmacString(dto.getTarget(), hmacKey.getEncryptedKey());
            UserDatum ud = switch (dto.getChannel()) {
                case SMS -> userDatumRepo.findByPhoneHash(channelHash)
                        .orElseThrow(() -> new BizException(BaseResponseCode.USER_NOT_FOUND));
                case EMAIL -> userDatumRepo.findByEmailHash(channelHash)
                        .orElseThrow(() -> new BizException(BaseResponseCode.USER_NOT_FOUND));
            };

            if (!dto.getNewPwd().equals(dto.getConfirmPwd())) {
                throw new BizException(BaseResponseCode.PASSWORD_TWO_PASSWORD_NOT_EQUAL);
            }

            userCoreService.changePwd(ud.getUid(), dto.getNewPwd());
            auditLogCoreService.recordSuccess(ud.getUid(), null,
                    AuditLogActionType.FORGOT_PASSWORD, deviceInfo);
        } catch (Exception e) {
            auditLogCoreService.recordFailure(null, dto.getTarget(), AuditLogActionType.FORGOT_PASSWORD,
                    e.getMessage(), deviceInfo);
            throw e;
        }
    }

    private LoginResult tryLogin(String username, boolean isAdmin,
                                  Supplier<User> credentialChecker, DeviceInfoReq deviceInfo) {
        String shortFailKey = LOGIN_FAIL_PREFIX + username;
        String dailyFailKey = LOGIN_FAIL_DAILY_PREFIX + username;
        checkShortTermLockout(shortFailKey, isAdmin);
        checkDailyLimit(dailyFailKey, isAdmin);

        User u;
        try {
            u = credentialChecker.get();
        } catch (AuthException e) {
            if (AuthCode.USERNAME_OR_PASSWORD_INCORRECT.getCode().equals(e.getErrorCode())) {
                incrementFailCounters(shortFailKey, dailyFailKey, isAdmin);
            }
            throw e;
        }

        boolean isUserAdmin = isAdmin(u.getUid());
        if (isAdmin != isUserAdmin) {
            throw new ForbiddenException();
        }

        redisHelper.del(shortFailKey);
        return recordAndBuildLoginResult(u, deviceInfo);
    }

    private LoginResult recordAndBuildLoginResult(User u, DeviceInfoReq deviceInfo) {
        siteStatisticRecorder.recordLogin();
        checkLoginBan(u.getUid());
        onlineUserService.updateLastActive(u.getUid());
        auditLogCoreService.recordSuccess(u.getUid(), u.getUsername(),
                AuditLogActionType.LOGIN, deviceInfo);
        return new LoginResult(u, false);
    }

    private void checkShortTermLockout(String failKey, boolean isAdmin) {
        String val = redisHelper.getValue(failKey);
        int threshold = isAdmin ? ADMIN_LOGIN_MAX_ATTEMPTS : NORMAL_LOGIN_MAX_ATTEMPTS;
        if (val != null && Integer.parseInt(val) >= threshold) {
            long lockMinutes = isAdmin ? ADMIN_LOGIN_LOCK_MINUTES : NORMAL_LOGIN_LOCK_MINUTES;
            throw new AttemptLimitExceededException(lockMinutes);
        }
    }

    private void checkDailyLimit(String failKey, boolean isAdmin) {
        String val = redisHelper.getValue(failKey);
        long threshold = isAdmin ? DAILY_ADMIN_LOGIN_MAX_ATTEMPTS : DAILY_NORMAL_LOGIN_MAX_ATTEMPTS;
        if (val != null && Long.parseLong(val) >= threshold) {
            log.warn("Daily login limit exceeded: username key={}", failKey);
            throw new DailyLimitExceededException();
        }
    }

    private void incrementFailCounters(String shortFailKey, String dailyFailKey, boolean isAdmin) {
        long lockMinutes = isAdmin ? ADMIN_LOGIN_LOCK_MINUTES : NORMAL_LOGIN_LOCK_MINUTES;
        redisHelper.increment(shortFailKey, Duration.ofMinutes(lockMinutes));
        redisHelper.increment(dailyFailKey, Duration.ofSeconds(DateUtil.getSecondsUntilMidnight()));
    }

    private User verifyCredentials(PwdLoginReq body, String verifyUuidKey) {
        return userRepo.findByUsername(body.getUsername())
                .map(uu -> {
                    if (!captchaService.verifyCode(verifyUuidKey, body.getCaptcha()))
                        throw new AuthException(AuthCode.CAPTCHA_INVALID);
                    if (!encoder.matches(body.getPassword(), uu.getPasswordHash()))
                        throw new AuthException(AuthCode.USERNAME_OR_PASSWORD_INCORRECT);
                    return uu;
                })
                .orElseThrow(() -> new AuthException(AuthCode.USERNAME_OR_PASSWORD_INCORRECT));
    }

    private void checkLoginBan(Long userUid) {
        for (UserPermission up : userPermRepo.findByUserUid(userUid)) {
            if (up.getPermission() == null || up.getExpiresAt() != null
                    && up.getExpiresAt().isBefore(java.time.Instant.now())) continue;
            if ("ban:login".equals(up.getPermission().getCode())) {
                log.warn("Login banned: uid={}, reason={}", userUid, up.getBanReasonType());
                throw new AuthException(AuthCode.REAUTHORIZATION_REQUIRED);
            }
        }
    }

    public boolean isAdmin(Long userUid) {
        return userRoleRepo.findByUserUid(userUid)
                .stream()
                .map(r -> r.getRole().getCode())
                .toList()
                .contains("ADMIN");
    }
}
