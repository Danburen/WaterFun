package org.waterwood.waterfunservicecore.services.auth.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.waterwood.api.AuthCode;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.api.auth.VerifyChannel;
import org.waterwood.waterfunservicecore.api.auth.VerifyScene;
import org.waterwood.waterfunservicecore.api.req.auth.VerifyCodeDto;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserDatum;
import org.waterwood.common.exceptions.AuthException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserDatumRepo;
import org.waterwood.waterfunservicecore.entity.security.BanReasonType;
import org.waterwood.waterfunservicecore.entity.user.UserPermission;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserPermRepo;
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

import java.util.Optional;

@Slf4j
@Service
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

    @Override
    public User login(PwdLoginReq body, String verifyUUIDKey){
        Optional<User> user = userRepo.findByUsername(body.getUsername());
        User u = user.map(uu -> {
            if(! captchaService.verifyCode(verifyUUIDKey, body.getCaptcha()))
                throw new AuthException(AuthCode.CAPTCHA_INVALID);
            if(! encoder.matches(body.getPassword(), uu.getPasswordHash()))
                throw new AuthException(AuthCode.USERNAME_OR_PASSWORD_INCORRECT);
            return uu;
        }).orElseThrow(() -> new AuthException(AuthCode.USERNAME_OR_PASSWORD_INCORRECT));
        siteStatisticRecorder.recordLogin();
        checkLoginBan(u.getUid());
        onlineUserService.updateLastActive(u.getUid());
        auditLogCoreService.record(u.getUid(), u.getUsername(), AuditLogActionType.LOGIN);
        return u;
    }

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
            userDatum = userDatumRepo.findByPhoneHash(HashUtil.Sha256HmacString(dto.getTarget(), hmacKey.getEncryptedKey()))
                    .orElse(null);
        }else if(channel == VerifyChannel.EMAIL){
            userDatum = userDatumRepo.findByEmailHash(HashUtil.Sha256HmacString(dto.getTarget(), hmacKey.getEncryptedKey()))
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

        verificationService.verifyCode(dto.getTarget(),dto.getScene(),channel,codeKey,dto.getCode());
        siteStatisticRecorder.recordLogin();
        User user = userDatum.getUser();
        checkLoginBan(user.getUid());
        onlineUserService.updateLastActive(user.getUid());
        auditLogCoreService.record(user.getUid(), user.getUsername(), AuditLogActionType.LOGIN);
        return new LoginResult(user, false);
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
}
