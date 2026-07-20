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
import org.waterwood.waterfunservicecore.api.req.auth.VerifyCodeDto;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserDatum;
import org.waterwood.common.exceptions.AuthException;
import org.waterwood.waterfunservicecore.exception.AttemptLimitExceededException;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.waterfunservicecore.exception.DailyLimitExceededException;
import org.waterwood.waterfunservicecore.exception.ForbiddenException;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelperHolder;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserDatumRepo;
import org.waterwood.waterfunservicecore.entity.security.BanReasonType;
import org.waterwood.waterfunservicecore.entity.user.UserPermission;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserPermRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRoleRepo;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptedKeyService;
import org.waterwood.waterfunservicecore.api.req.auth.PwdLoginReq;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.entity.EncryptionDataKey;
import org.waterwood.waterfunservicecore.infrastructure.security.RefreshTokenPayload;
import org.waterwood.utils.codec.HashUtil;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogActionType;
import org.waterwood.waterfunservicecore.services.auth.LoginService;
import org.waterwood.waterfunservicecore.services.auth.RegisterService;
import org.waterwood.waterfunservicecore.services.auth.code.VerificationService;
import org.waterwood.waterfunservicecore.services.audit.AuditLogCoreService;
import org.waterwood.waterfunservicecore.services.online.OnlineUserService;
import org.waterwood.waterfunservicecore.services.stats.SiteStatisticRecorder;

import java.time.Duration;
import java.util.Optional;
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
                    dto.getTarget(), channel, dto.getScene(), codeKey, dto.getCode(), dto.getDeviceFp()
            );
            siteStatisticRecorder.recordLogin();
            checkLoginBan(autoUser.getUid());
            onlineUserService.updateLastActive(autoUser.getUid());
            auditLogCoreService.record(autoUser.getUid(), autoUser.getUsername(), AuditLogActionType.LOGIN);
            return new LoginResult(autoUser, true);
        }
        User u = userDatum.getUser();
        verificationService.verifyCode(dto.getTarget(),dto.getScene(),channel,codeKey,dto.getCode());
        // no-admin allow login
        if(! isAdmin(u.getUid())){
            return recordAndBuildLoginResult(u);
        } else {
            throw new ForbiddenException();
        }
    }

    @Override
    public LoginResult adminLogin(PwdLoginReq body, String verifyUuidKey){
        return tryLogin(body.getUsername(), true,
                () -> verifyCredentials(body, verifyUuidKey));
    }

    @Override
    public LoginResult login(PwdLoginReq body, String verifyUuidKey){
        return tryLogin(body.getUsername(), false,
                () -> verifyCredentials(body, verifyUuidKey));
    }

    /**
     * Unified login gate for both admin and normal users.
     * <p>Checks short-term and daily attempt limits, runs the credential checker
     * (which produces the authenticated User), verifies login-type / user-type match,
     * records the audit trail, and clears fail counters on success.</p>
     *
     * @param username          login username (for Redis fail keys)
     * @param isAdmin           whether this is an admin login attempt
     * @param credentialChecker produces the authenticated {@link User} on success
     * @return {@link LoginResult}
     */
    private LoginResult tryLogin(String username, boolean isAdmin, Supplier<User> credentialChecker) {
        String shortFailKey = LOGIN_FAIL_PREFIX + username;
        String dailyFailKey = LOGIN_FAIL_DAILY_PREFIX + username;
        // Short-term lockout: N consecutive failures within the lock window
        checkShortTermLockout(shortFailKey, isAdmin);
        // Daily cumulative limit
        checkDailyLimit(dailyFailKey, isAdmin);
        // Run the credential checker — it authenticates and returns the User
        User u;
        try {
            u = credentialChecker.get();
        } catch (AuthException e) {
            // Count password/username errors only (not captcha errors)
            if (AuthCode.USERNAME_OR_PASSWORD_INCORRECT.getCode().equals(e.getErrorCode())) {
                incrementFailCounters(shortFailKey, dailyFailKey, isAdmin);
            }
            throw e;
        }
        // Verify the login type matches the user's actual role
        boolean isUserAdmin = isAdmin(u.getUid());
        if (isAdmin != isUserAdmin) {
            throw new ForbiddenException();
        }
        // Success — clear counters and record
        redisHelper.del(shortFailKey);;
        return recordAndBuildLoginResult(u);
    }

    private LoginResult recordAndBuildLoginResult(User u) {
        siteStatisticRecorder.recordLogin();
        checkLoginBan(u.getUid());
        onlineUserService.updateLastActive(u.getUid());
        auditLogCoreService.record(u.getUid(), u.getUsername(), AuditLogActionType.LOGIN);
        return new LoginResult(u, false);
    }

    private void checkShortTermLockout(String failKey, boolean isAdmin) {
        String val = redisHelper.getValue(failKey);
        int threshold = isAdmin ? ADMIN_LOGIN_MAX_ATTEMPTS : NORMAL_LOGIN_MAX_ATTEMPTS;
        if (val != null && Integer.parseInt(val) > threshold) {
            long lockMinutes = isAdmin ? ADMIN_LOGIN_LOCK_MINUTES : NORMAL_LOGIN_LOCK_MINUTES;
            throw new AttemptLimitExceededException(lockMinutes);
        }
    }

    private void checkDailyLimit(String failKey, boolean isAdmin) {
        String val = redisHelper.getValue(failKey);
        long threshold = isAdmin ? DAILY_ADMIN_LOGIN_MAX_ATTEMPTS : DAILY_NORMAL_LOGIN_MAX_ATTEMPTS;
        if (val != null && Long.parseLong(val) > threshold) {
            log.warn("Daily login limit exceeded: username key={}", failKey);
            throw new DailyLimitExceededException();
        }
    }

    private void incrementFailCounters(String shortFailKey, String dailyFailKey, boolean isAdmin) {
        long lockMinutes = isAdmin ? ADMIN_LOGIN_LOCK_MINUTES : NORMAL_LOGIN_LOCK_MINUTES;
        redisHelper.increment(shortFailKey, Duration.ofMinutes(lockMinutes));
        redisHelper.increment(dailyFailKey, Duration.ofSeconds(DateUtil.getSecondsUntilMidnight()));
    }

    /**
     * Verify captcha and password credentials, returning the authenticated User.
     * Throws AuthException with specific error codes:
     * - CAPTCHA_INVALID if captcha is wrong
     * - USERNAME_OR_PASSWORD_INCORRECT if user not found or password mismatch
     */
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
                BanReasonType reason = up.getBanReasonType() != null ? up.getBanReasonType() : BanReasonType.UNSPECIFIED;
                String messageKey = reason.getMessageKey() != null ? reason.getMessageKey() : "http.forbidden";
                log.warn("Login banned: uid={}, reason={}", userUid, reason);
                throw new AuthException(AuthCode.REAUTHORIZATION_REQUIRED);
            }
        }
    }

    public boolean isAdmin(Long userUid) {
        return userRoleRepo.findByUserUid(userUid)
                .stream()
                .map(r-> r.getRole().getCode())
                .toList()
                .contains("ADMIN");
    }
}
