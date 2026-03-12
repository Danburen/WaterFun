package org.waterwood.waterfunservicecore.services.user;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.BizException;
import org.waterwood.utils.MaskUtil;
import org.waterwood.utils.codec.HashUtil;
import org.waterwood.waterfunservicecore.api.resp.AccountResp;
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
                .orElseThrow(() -> new BizException(BaseResponseCode.USER_NOT_FOUND));
    }

    @Override
    public UserDatum saveNewEmail(long userUid, String email, boolean verified) {
        UserDatum ud = getUserDatum(userUid);
        EncryptionDataKey hmacKey = encryptedKeyService.getUserDatumHmacKey();
        String newHashed = HashUtil.Sha256HmacString(email, hmacKey.getEncryptedKey());
        // check new email whether equal to old email
        if(newHashed.equals(ud.getEmailHash())){
            throw new BizException(BaseResponseCode.TWO_VALUE_MUST_DIFFERENT,"email");
        }
        // check new email whether bound by others
        userDatumRepo.findByEmailHash(newHashed).ifPresent(_ ->{
            throw new BizException(BaseResponseCode.EMAIL_ALREADY_USED);
        });
        // save to the db
        EncryptionDataKey aesKey = encryptedKeyService.getAesKey();
        ud.setEmailEncrypted(EncryptionHelper.encryptField(email, aesKey));
        ud.setEmailHash(newHashed);
        ud.setEmailVerified(verified);
        return userDatumRepo.save(ud);
    }

    @Override
    public UserDatum saveNewPhone(long userUid, String phone, boolean verified) {
        UserDatum ud = getUserDatum(userUid);
        EncryptionDataKey hmacKey = encryptedKeyService.getUserDatumHmacKey();
        String newHashed = HashUtil.Sha256HmacString(phone, hmacKey.getEncryptedKey());
        // check new phone whether equal to old phone
        if(newHashed.equals(ud.getPhoneHash())){
            throw new BizException(BaseResponseCode.TWO_VALUE_MUST_DIFFERENT,"phone");
        }
        // check new phone whether bound by others
        userDatumRepo.findByPhoneHash(newHashed).ifPresent(_ ->{
            throw new BizException(BaseResponseCode.PHONE_NUMBER_ALREADY_USED);
        });
        // save to the db
        EncryptionDataKey aesKey = encryptedKeyService.getAesKey();
        ud.setPhoneEncrypted(EncryptionHelper.encryptField(phone, aesKey));
        ud.setPhoneHash(newHashed);
        ud.setPhoneVerified(verified);
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

    @Override
    @Transactional
    public AccountResp getAccountInfo(long userUid) {
        UserDatum ud = userDatumRepo.findUserDatumByUserUid(userUid)
                .orElseThrow(() -> new BizException(BaseResponseCode.USER_NOT_FOUND));
        EncryptionDataKey aesKey = encryptedKeyService.pickEncryptionKey(0);
        String realEmail = EncryptionHelper.decryptField(ud.getEmailEncrypted(), aesKey);
        String realPhone = EncryptionHelper.decryptField(ud.getPhoneEncrypted(), aesKey);
        return new AccountResp(
                MaskUtil.maskPhone(realPhone),
                MaskUtil.maskEmail(realEmail),
                ud.getPhoneVerified(),
                ud.getEmailVerified()
        );
    }

}
