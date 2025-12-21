package org.waterwood.waterfunservicecore.services.user;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.BusinessException;
import org.waterwood.utils.codec.HashUtil;
import org.waterwood.waterfunservicecore.entity.user.UserDatum;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserDatumRepo;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptedKeyService;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionDataKey;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionHelper;

@Service
@RequiredArgsConstructor
public class UserDatumCoreServiceImpl implements UserDatumCoreService {
    private final UserDatumRepo userDatumRepo;
    private final EncryptedKeyService encryptedKeyService;

    @Override
    public UserDatum getUserDatum(long userUid) {
        return userDatumRepo.findUserDatumByUserUid(userUid)
                .orElseThrow(() -> new BusinessException(BaseResponseCode.USER_NOT_FOUND));
    }

    @Override
    public UserDatum saveNewEmailVerified(long userUid, String email) {
        UserDatum ud = getUserDatum(userUid);
        EncryptionDataKey hmacKey = encryptedKeyService.getUserDatumHmacKey();
        String newHashed = HashUtil.Sha256HmacString(email, hmacKey.getEncryptedKey());
        // check new email whether equal to old email
        if(newHashed.equals(ud.getEmailHash())){
            throw new BusinessException(BaseResponseCode.TWO_VALUE_MUST_DIFFERENT,"email");
        }
        // check new email whether bound by others
        userDatumRepo.findByEmailHash(newHashed).ifPresent(_ ->{
            throw new BusinessException(BaseResponseCode.EMAIL_ALREADY_USED);
        });
        // save to the db
        EncryptionDataKey aesKey = encryptedKeyService.getAesKey();
        ud.setEmailEncrypted(EncryptionHelper.encryptField(email, aesKey));
        ud.setEmailHash(newHashed);
        ud.setEmailVerified(true);
        return userDatumRepo.save(ud);
    }

    @Override
    public UserDatum saveNewPhoneVerified(long userUid, String phone) {
        UserDatum ud = getUserDatum(userUid);
        EncryptionDataKey hmacKey = encryptedKeyService.getUserDatumHmacKey();
        String newHashed = HashUtil.Sha256HmacString(phone, hmacKey.getEncryptedKey());
        // check new phone whether equal to old phone
        if(newHashed.equals(ud.getPhoneHash())){
            throw new BusinessException(BaseResponseCode.TWO_VALUE_MUST_DIFFERENT,"phone");
        }
        // check new phone whether bound by others
        userDatumRepo.findByPhoneHash(newHashed).ifPresent(_ ->{
            throw new BusinessException(BaseResponseCode.PHONE_NUMBER_ALREADY_USED);
        });
        // save to the db
        EncryptionDataKey aesKey = encryptedKeyService.getAesKey();
        ud.setPhoneEncrypted(EncryptionHelper.encryptField(phone, aesKey));
        ud.setPhoneHash(newHashed);
        ud.setPhoneVerified(true);
        return userDatumRepo.save(ud);
    }

    @Override
    public String getRawPhone(long userUid) {
        return EncryptionHelper.decryptField(
                getUserDatum(userUid).getPhoneEncrypted(),
                encryptedKeyService.getAesKey()
        );
    }

    @Override
    public @Nullable String getRawEmail(long userUid) {
        UserDatum ud = getUserDatum(userUid);
        String emailEncrypted = ud.getEmailEncrypted();
        if(emailEncrypted == null){
            return null;
        }
        return EncryptionHelper.decryptField(
                getUserDatum(userUid).getEmailEncrypted(),
                encryptedKeyService.getAesKey()
        );
    }

}
