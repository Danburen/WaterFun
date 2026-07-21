package org.waterwood.waterfunservicecore.services.auth.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.AuthCode;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.api.auth.VerifyChannel;
import org.waterwood.waterfunservicecore.api.auth.VerifyScene;
import org.waterwood.waterfunservicecore.api.req.auth.VerifyCodeDto;
import org.waterwood.waterfunservicecore.entity.user.*;
import org.waterwood.waterfunservicecore.exception.RegisterChannelUnsupportedException;
import org.waterwood.waterfunservicecore.exception.UserNameAlreadyExistException;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionHelper;
import org.waterwood.waterfunservicecore.entity.EncryptionDataKey;
import org.waterwood.common.exceptions.AuthException;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserDatumRepo;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptedKeyService;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.AuthContext;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.api.req.auth.DeviceInfoReq;
import org.waterwood.waterfunservicecore.api.req.auth.RegisterRequest;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.utils.codec.HashUtil;
import org.waterwood.utils.StringUtil;
import org.waterwood.utils.UidGenerator;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogActionType;
import org.waterwood.waterfunservicecore.services.audit.AuditLogCoreService;
import org.waterwood.waterfunservicecore.services.auth.RegisterService;
import org.waterwood.waterfunservicecore.services.auth.code.VerificationService;
import org.waterwood.waterfunservicecore.services.stats.SiteStatisticRecorder;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RegisterServiceImpl implements RegisterService {
    private final AuthCoreServiceImpl authService;
    private final UserRepository userRepo;
    private final UserDatumRepo userDatumRepo;
    private final EncryptedKeyService encryptedKeyService;
    private final UidGenerator uidGenerator;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final VerificationService verificationService;
    private final SiteStatisticRecorder siteStatisticRecorder;
    private final AuditLogCoreService auditLogCoreService;

    @Value("${expiresIn.email.unverified-expiresIn-hours:24}")
    private Long emailUnverifiedExpireHours;

    @Transactional
    @Override
    public User register(RegisterRequest body, String smsCodeKey) {
        DeviceInfoReq deviceInfo = body.getVerify() != null ? body.getVerify().getDeviceInfo() : null;
        try {
            String email = body.getEmail();
            String phone = body.getPhone();
            EncryptionDataKey hmacKey = encryptedKeyService.getUserDatumHmacKey();

            userRepo.findByUsername(body.getUsername()).ifPresent(_ -> {
                throw new UserNameAlreadyExistException();
            });
            // Verify phone
            VerifyCodeDto verify = body.getVerify();
            if(! verify.getTarget().equals(phone)){
                throw new AuthException(AuthCode.REAUTHORIZATION_REQUIRED);
            }
            verificationService.verifyCode(smsCodeKey, verify);

            userDatumRepo.findByPhoneHash(HashUtil.toSha256HmacString(phone, hmacKey.getEncryptedKey())).ifPresent(
                    _->{
                        throw new BizException(BaseResponseCode.PHONE_NUMBER_ALREADY_USED);
                    }
            );
            // Verify email
            if (StringUtil.isNotBlank(email)) {
                userDatumRepo.findByEmailHash(HashUtil.toSha256HmacString(email, hmacKey.getEncryptedKey())).ifPresent(
                        _ -> {
                            throw new BizException(BaseResponseCode.EMAIL_ALREADY_USED);
                        }
                );
            }
            // Encrypt email, phone, password
            EncryptionDataKey aesKet = encryptedKeyService.getAesKey();
            String encryptedPhone = EncryptionHelper.encryptField(phone, aesKet);
            String password = body.getPassword();

            // STEP 5: Set user
            User user = new User();
            user.setUsername(body.getUsername());
            if(StringUtil.isNotBlank( password)) user.setPasswordHash(encoder.encode(password));
            user.setUid(uidGenerator.generateUid());
            user.setAccountStatus(AccountStatus.ACTIVE);
            // STEP 6: Set user data
            UserDatum ud = new UserDatum();
            ud.setUser(user);
            ud.setUid(user.getUid());
            ud.setEncryptionKeyId(aesKet.getKeyId());
            ud.setPhoneEncrypted(encryptedPhone);
            ud.setPhoneHash(HashUtil.toSha256HmacString(phone, hmacKey.getEncryptedKey()));

            if(StringUtil.isNotBlank(email)) {
                String encryptedEmail = EncryptionHelper.encryptField(email, aesKet);
                ud.setEmailEncrypted(encryptedEmail);
                ud.setEmailHash(HashUtil.toSha256HmacString(email, hmacKey.getEncryptedKey()));
                ud.setEmailExpireAt(Instant.now().plus(Duration.ofHours(emailUnverifiedExpireHours)));
            }

            ud.setPhoneVerified(true);
            ud.setEmailVerified(false);

            UserProfile up = new UserProfile();
            up.setUser(user);
            UserCounter uc = new UserCounter();
            uc.setUser(user);
            UserPreference upp = new UserPreference();
            upp.setUser(user);
            upp.setLocale(UserCtxHolder.safeGet().map(AuthContext::getLocale).orElse(Locale.CHINA).toLanguageTag());

            UserSetting us = new UserSetting();
            us.setUser(user);

            user.setUserCounter(uc);
            user.setUserProfile(up);
            user.setUserDatum(ud);
            user.setUserPreference(upp);
            user.setUserSetting(us);
            userRepo.save(user);
            siteStatisticRecorder.recordNewUser();
            auditLogCoreService.recordSuccess(user.getUid(), user.getUsername(),
                    AuditLogActionType.REGISTER, deviceInfo);
            return user;
        } catch (Exception e) {
            auditLogCoreService.recordFailure(null, body.getUsername(), AuditLogActionType.REGISTER,
                    e.getMessage(), deviceInfo);
            throw e;
        }
    }

    @Transactional
    @Override
    public User autoRegister(String target, VerifyChannel channel, VerifyScene scene, String codeKey, String code, String deviceFp) {
        if (channel == VerifyChannel.EMAIL) {
            throw new RegisterChannelUnsupportedException();
        }

        EncryptionDataKey hmacKey = encryptedKeyService.getUserDatumHmacKey();

        verificationService.verifyCode(target, scene, channel, codeKey, code);

        if (channel == VerifyChannel.SMS) {
            userDatumRepo.findByPhoneHash(HashUtil.toSha256HmacString(target, hmacKey.getEncryptedKey())).ifPresent(
                    _ -> { throw new BizException(BaseResponseCode.PHONE_NUMBER_ALREADY_USED); }
            );
        } else if (channel == VerifyChannel.EMAIL) {
            userDatumRepo.findByEmailHash(HashUtil.toSha256HmacString(target, hmacKey.getEncryptedKey())).ifPresent(
                    _ -> { throw new BizException(BaseResponseCode.EMAIL_ALREADY_USED); }
            );
        }

        String username;
        int suffix = 0;
        do {
            String shortHash = HashUtil.toSha256HmacString(target, hmacKey.getEncryptedKey()).substring(0, 8);
            String suffixStr = suffix > 0 ? String.valueOf(suffix) : "";
            username = "u_" + shortHash + suffixStr;
            suffix++;
        } while (userRepo.findByUsername(username).isPresent());

        EncryptionDataKey aesKet = encryptedKeyService.getAesKey();
        String encryptedTarget = EncryptionHelper.encryptField(target, aesKet);

        User user = new User();
        user.setUsername(username);
        user.setUid(uidGenerator.generateUid());
        user.setAccountStatus(AccountStatus.ACTIVE);

        UserDatum ud = new UserDatum();
        ud.setUser(user);
        ud.setUid(user.getUid());
        ud.setEncryptionKeyId(aesKet.getKeyId());

        if (channel == VerifyChannel.SMS) {
            ud.setPhoneEncrypted(encryptedTarget);
            ud.setPhoneHash(HashUtil.toSha256HmacString(target, hmacKey.getEncryptedKey()));
            ud.setPhoneVerified(true);
        } else {
            ud.setEmailEncrypted(encryptedTarget);
            ud.setEmailHash(HashUtil.toSha256HmacString(target, hmacKey.getEncryptedKey()));
            ud.setEmailVerified(false);
            ud.setEmailExpireAt(Instant.now().plus(Duration.ofHours(emailUnverifiedExpireHours)));
        }

        UserProfile up = new UserProfile();
        up.setUser(user);
        UserCounter uc = new UserCounter();
        uc.setUser(user);
        UserPreference upp = new UserPreference();
        upp.setUser(user);
        upp.setLocale(UserCtxHolder.safeGet().map(AuthContext::getLocale).orElse(Locale.CHINA).toLanguageTag());
        UserSetting us = new UserSetting();
        us.setUser(user);

        user.setUserCounter(uc);
        user.setUserProfile(up);
        user.setUserDatum(ud);
        user.setUserPreference(upp);
        user.setUserSetting(us);
        userRepo.save(user);
        siteStatisticRecorder.recordNewUser();
        auditLogCoreService.recordSuccess(user.getUid(), user.getUsername(), AuditLogActionType.REGISTER);
        return user;
    }
}
