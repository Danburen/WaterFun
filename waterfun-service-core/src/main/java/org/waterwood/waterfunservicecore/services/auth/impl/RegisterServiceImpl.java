package org.waterwood.waterfunservicecore.services.auth.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.api.req.auth.VerifyCodeDto;
import org.waterwood.waterfunservicecore.entity.user.*;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserCounterRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserProfileRepo;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionHelper;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionDataKey;
import org.waterwood.common.exceptions.AuthException;
import org.waterwood.common.exceptions.BizException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserDatumRepo;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptedKeyService;
import org.waterwood.waterfunservicecore.api.req.auth.RegisterRequest;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.utils.codec.HashUtil;
import org.waterwood.utils.StringUtil;
import org.waterwood.utils.UidGenerator;
import org.waterwood.waterfunservicecore.services.auth.RegisterService;
import org.waterwood.waterfunservicecore.services.auth.code.VerificationService;
import org.waterwood.waterfunservicecore.services.sms.SmsCodeService;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterServiceImpl implements RegisterService {
    private final AuthServiceImpl authService;
    private final UserRepository userRepo;
    private final UserDatumRepo userDatumRepo;
    private final EncryptedKeyService encryptedKeyService;
    private final UidGenerator uidGenerator;
    private final SmsCodeService smsCodeService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UserProfileRepo userProfileRepo;
    private final VerificationService verificationService;
    private final UserCounterRepository userCounterRepository;

    @Value("${expire.email.unverified-expire-hours:24}")
    private Long emailUnverifiedExpireHours;

    @Transactional
    @Override
    public User register(RegisterRequest body, String smsCodeKey) {
        String email = body.getEmail();
        String phone = body.getPhone();
        EncryptionDataKey hmacKey = encryptedKeyService.getUserDatumHmacKey();

        // STEP 1: Verify phone
        VerifyCodeDto verify = body.getVerify();
        if(! verify.getTarget().equals(phone)){
            throw new AuthException(BaseResponseCode.VERIFY_TARGET_UNSUPPORTED);
        }
        verificationService.verifyCode(smsCodeKey, verify);

        userDatumRepo.findByPhoneHash(HashUtil.Sha256HmacString(phone, hmacKey.getEncryptedKey())).ifPresent(
                _->{
                    throw new AuthException(BaseResponseCode.PHONE_NUMBER_ALREADY_USED);
                }
        );
        // STEP 2: Verify email
        boolean emailNotBlank = StringUtil.isNotBlank(email);
        if (emailNotBlank) {
            userDatumRepo.findByEmailHash(HashUtil.Sha256HmacString(email, hmacKey.getEncryptedKey())).ifPresent(
                    _ -> {
                        throw new AuthException(BaseResponseCode.EMAIL_ALREADY_USED);
                    }
            );
        }

        // STEP 3: Verify user whether  exists
        userRepo.findByUsername(body.getUsername()).ifPresent(_ -> {
            throw new BizException(BaseResponseCode.USER_ALREADY_EXISTS);
        });

        // STEP 4: Encrypt email, phone, password
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
        ud.setPhoneHash(HashUtil.Sha256HmacString(phone, hmacKey.getEncryptedKey()));

        if(emailNotBlank) {
            String encryptedEmail = EncryptionHelper.encryptField(email, aesKet);
            ud.setEmailEncrypted(encryptedEmail);
            ud.setEmailHash(HashUtil.Sha256HmacString(email, hmacKey.getEncryptedKey()));
            ud.setEmailExpireAt(Instant.now().plus(Duration.ofHours(emailUnverifiedExpireHours)));
        }

        ud.setPhoneVerified(true);
        ud.setEmailVerified(false);

        UserProfile up = new UserProfile();
        up.setUser(user);
        UserCounter uc = new UserCounter();
        uc.setUser(user);

        user.setUserCounter(uc);
        user.setUserProfile(up);
        user.setUserDatum(ud);
        userRepo.save(user);
        return user;
    }
}
