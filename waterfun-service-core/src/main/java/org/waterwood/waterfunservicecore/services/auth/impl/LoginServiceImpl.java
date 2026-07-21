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
import org.waterwood.waterfunservicecore.exception.notfound.NotFoundException;
import org.waterwood.waterfunservicecore.services.user.UserDatumCoreService;
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
import org.waterwood.waterfunservicecore.services.auth.SingleUseTokenService;
import org.waterwood.waterfunservicecore.services.auth.RegisterService;
import org.waterwood.waterfunservicecore.services.auth.code.VerificationService;
import org.waterwood.waterfunservicecore.services.online.OnlineUserService;
import org.waterwood.waterfunservicecore.services.stats.SiteStatisticRecorder;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;
import org.waterwood.common.cache.RedisKeyBuilder;
import static org.waterwood.common.RedisKeyPrefix.THRESHOLD;
import org.waterwood.utils.codec.HashUtil;

import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {
    private final UserRepository userRepo;
    private final AccessTokenServiceImpl authTokenServiceImpl;
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
    private final UserDatumCoreService userDatumCoreService;
    private final SingleUseTokenService singleUseTokenService;

    private static final int ADMIN_LOGIN_MAX_ATTEMPTS = 5;
    private static final long ADMIN_LOGIN_LOCK_MINUTES = 120;
    private static final int NORMAL_LOGIN_MAX_ATTEMPTS = 5;
    private static final long NORMAL_LOGIN_LOCK_MINUTES = 15;
    private static final long DAILY_NORMAL_LOGIN_MAX_ATTEMPTS = 30;
    private static final long DAILY_ADMIN_LOGIN_MAX_ATTEMPTS = 15;

    // -- Redis key builders --

    /** Short-term lockout window key: {@code threshold:fail:login:temp:{username}} */
    private static String loginTempFailKey(String username) {
        return RedisKeyBuilder.build(THRESHOLD, "fail", "login", "temp", username);
    }

    /** Daily limit window key: {@code threshold:fail:login:daily:{username}} */
    private static String loginDailyFailKey(String username) {
        return RedisKeyBuilder.build(THRESHOLD, "fail", "login", "daily", username);
    }

    @Override
    public void logout(String refreshToken, String dfp) {
        if(StringUtil.isBlank(refreshToken)) throw new AuthException(AuthCode.REAUTHORIZATION_REQUIRED);
        long userUid = UserCtxHolder.getUserUid();
        RefreshTokenPayload payload = authTokenServiceImpl.validateRefreshToken(userUid, refreshToken, dfp);
        authTokenServiceImpl.removeAccessToken(payload.userUid(), payload.deviceId());
        authTokenServiceImpl.removeRefreshToken(userUid, dfp, refreshToken);
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
            // Resolve identifier → find the user
            Long userUid = userCoreService.resolveUid(dto.getTarget());

            // Get the bound phone number for SMS verification
            String phone = userDatumCoreService.getRawPhone(userUid);
            if (phone == null) {
                throw new BizException(BaseResponseCode.USER_NOT_FOUND);
            }

            // Verify the SMS code against the bound phone (channel forced to SMS)
            verificationService.verifyCode(
                    phone, VerifyScene.FORGOT_PASSWORD, VerifyChannel.SMS,
                    verifyCodeKey, dto.getCode()
            );

            if (!dto.getNewPwd().equals(dto.getConfirmPwd())) {
                throw new BizException(BaseResponseCode.PASSWORD_TWO_PASSWORD_NOT_EQUAL);
            }

            userCoreService.changePwd(userUid, dto.getNewPwd());
            auditLogCoreService.recordSuccess(userUid, null,
                    AuditLogActionType.FORGOT_PASSWORD, deviceInfo);
        } catch (NotFoundException e) {
            auditLogCoreService.recordFailure(null, dto.getTarget(), AuditLogActionType.FORGOT_PASSWORD,
                    "User not found", deviceInfo);
            throw new BizException(BaseResponseCode.USER_NOT_FOUND);
        } catch (Exception e) {
            auditLogCoreService.recordFailure(null, dto.getTarget(), AuditLogActionType.FORGOT_PASSWORD,
                    e.getMessage(), deviceInfo);
            throw e;
        }
    }

    private LoginResult tryLogin(String username, boolean isAdmin,
                                  Supplier<User> credentialChecker, DeviceInfoReq deviceInfo) {
        String shortFailKey = loginTempFailKey(username);
        String dailyFailKey = loginDailyFailKey(username);
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
        // Resolve identifier (phone/email/username) → userUid → User
        Long userUid;
        try {
            userUid = userCoreService.resolveUid(body.getUsername());
        } catch (NotFoundException e) {
            throw new AuthException(AuthCode.USERNAME_OR_PASSWORD_INCORRECT);
        }
        User uu = userCoreService.getUser(userUid);
        if (!captchaService.verifyCode(verifyUuidKey, body.getCaptcha()))
            throw new AuthException(AuthCode.CAPTCHA_INVALID);
        if (!encoder.matches(body.getPassword(), uu.getPasswordHash()))
            throw new AuthException(AuthCode.USERNAME_OR_PASSWORD_INCORRECT);
        return uu;
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
