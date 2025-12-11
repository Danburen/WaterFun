package org.waterwood.waterfunservicecore.services.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionHelper;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionDataKey;
import org.waterwood.waterfunservicecore.entity.user.AccountStatus;
import org.waterwood.common.exceptions.AuthException;
import org.waterwood.common.exceptions.BusinessException;
import org.waterwood.common.exceptions.ServiceException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserDatumRepo;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptedKeyService;
import org.waterwood.waterfunservicecore.api.req.auth.RegisterRequest;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserDatum;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.utils.codec.HashUtil;
import org.waterwood.utils.StringUtil;
import org.waterwood.utils.UidGenerator;
import org.waterwood.waterfunservicecore.services.sms.SmsCodeService;

import java.util.List;

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

    @Transactional
    @Override
    public User register(RegisterRequest body, String smsCodeKey) {
        List<EncryptionDataKey> keys = encryptedKeyService.pickEncryptionKeys(0, 1)
                .orElseThrow(() -> new ServiceException("No encryption key available"));

        String email = body.getEmail();
        String phone = body.getPhone();
        EncryptionDataKey hmacKey = keys.get(1);

        // STEP 1: Verify phone
        boolean phoneVerified = smsCodeService.verifyCode(
                phone,
                body.getVerify().getScene(),
                smsCodeKey,
                body.getVerify().getCode());
        if(! phoneVerified){
            throw new AuthException(BaseResponseCode.VERIFY_CODE_INVALID);
        }
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
            throw new BusinessException(BaseResponseCode.USER_ALREADY_EXISTS);
        });

        // STEP 4: Encrypt email, phone, password
        EncryptionDataKey encryptionKey = keys.get(0);
        String encryptedPhone = EncryptionHelper.encryptField(phone, encryptionKey);
        String password = body.getPassword();

        // STEP 5: Set user
        User user = new User();
        user.setUsername(body.getUsername());
        user.setPasswordHash(encoder.encode(password));
        user.setUid(uidGenerator.generateUid());
        user.setAccountStatus(AccountStatus.ACTIVE);
        // STEP 6: Set user data
        UserDatum userDatum = new UserDatum();
        userDatum.setUser(user);
        userDatum.setId(user.getId());
        userDatum.setEncryptionKeyId(encryptionKey.getId());
        userDatum.setPhoneEncrypted(encryptedPhone);
        userDatum.setPhoneHash(HashUtil.Sha256HmacString(phone, keys.get(1).getEncryptedKey()));

        if(emailNotBlank) {
            String encryptedEmail = EncryptionHelper.encryptField(email, encryptionKey);
            userDatum.setEmailEncrypted(encryptedEmail);
            userDatum.setEmailHash(HashUtil.Sha256HmacString(email, keys.get(1).getEncryptedKey()));
        }

        userDatum.setPhoneVerified(true);
        userRepo.save(user);
        userDatumRepo.save(userDatum);
        return user;
    }
}
