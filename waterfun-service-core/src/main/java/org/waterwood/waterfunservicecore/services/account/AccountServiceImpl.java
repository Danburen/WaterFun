package org.waterwood.waterfunservicecore.services.account;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.BusinessException;
import org.waterwood.common.exceptions.ServiceException;
import org.waterwood.utils.codec.HashUtil;
import org.waterwood.waterfunservicecore.api.VerifyChannel;
import org.waterwood.waterfunservicecore.api.VerifyScene;
import org.waterwood.waterfunservicecore.api.req.EmailChangeDto;
import org.waterwood.waterfunservicecore.api.req.ResetPasswordDto;
import org.waterwood.waterfunservicecore.api.req.SetPasswordDto;
import org.waterwood.waterfunservicecore.api.req.EmailBindActivateDto;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserDatum;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserDatumRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserProfileRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptedKeyService;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionDataKey;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionHelper;
import org.waterwood.waterfunservicecore.services.auth.code.VerificationService;

import java.util.List;

@Service
public class AccountServiceImpl implements AccountService{

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final VerificationService verificationService;
    private final UserDatumRepo userDatumRepo;
    private final UserProfileRepo userProfileRepo;
    private final EncryptedKeyService encryptedKeyService;

    public AccountServiceImpl(UserRepository userRepository, VerificationService verificationService, UserDatumRepo userDatumRepo, UserProfileRepo userProfileRepo, EncryptedKeyService encryptedKeyService) {
        this.userRepository = userRepository;
        this.verificationService = verificationService;
        this.userDatumRepo = userDatumRepo;
        this.userProfileRepo = userProfileRepo;
        this.encryptedKeyService = encryptedKeyService;
    }

    @Override
    public void changePwd(Long userId, String verifyCodeKey,ResetPasswordDto dto) {
        VerifyScene scene = dto.getVerify().getScene();
        verificationService.verifyCode(verifyCodeKey, dto.getVerify());
        if(scene != VerifyScene.RESET_PASSWORD) { // scene invalid
            throw new BusinessException(BaseResponseCode.INVALID_VERIFY_SCENE);
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(BaseResponseCode.USER_NOT_FOUND));
        if(! encoder.matches(dto.getOldPwd(), user.getPasswordHash())){
            throw new BusinessException(BaseResponseCode.OLD_PASSWORD_INCORRECT);
        }

        if(! dto.getConfirmPwd().equals(dto.getNewPwd())){
            throw new BusinessException(BaseResponseCode.PASSWORD_TWO_PASSWORD_NOT_EQUAL);
        }

        user.setPasswordHash(encoder.encode(dto.getNewPwd()));
        userRepository.save(user);
    }

    @Override
    public void setPassword(Long userId, String verifyCodeKey, SetPasswordDto dto) {
        verificationService.verifyCode(verifyCodeKey, dto.getVerify());
        if(dto.getVerify().getScene() != VerifyScene.SET_PASSWORD) { // scene invalid
            throw new BusinessException(BaseResponseCode.INVALID_VERIFY_SCENE);
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(BaseResponseCode.USER_NOT_FOUND));
        if(! dto.getConfirmPwd().equals(dto.getNewPwd())){
            throw new BusinessException(BaseResponseCode.PASSWORD_TWO_PASSWORD_NOT_EQUAL);
        }
        if(user.getPasswordHash() != null) {
            throw new BusinessException(BaseResponseCode.PASSWORD_ALREADY_SET);
        }
        user.setPasswordHash(encoder.encode(dto.getNewPwd()));
        userRepository.save(user);
    }


    @Override
    public void bindOrActivateEmail(Long userId, String verifyCodeKey, EmailBindActivateDto dto) {
        verificationService.verifyCode(verifyCodeKey, dto.getVerify());
        VerifyScene scene = dto.getVerify().getScene();
        // must be email and email channel
        if(!(scene == VerifyScene.BIND_EMAIL  || scene == VerifyScene.ACTIVATE_EMAIL)
                || dto.getVerify().getChannel() != VerifyChannel.EMAIL){
            throw new BusinessException(BaseResponseCode.INVALID_VERIFY_SCENE);
        }

        UserDatum ud = userDatumRepo.findUserDatumByUserId(userId)
                .orElseThrow(() -> new BusinessException(BaseResponseCode.USER_NOT_FOUND));
        // Get encrypted keys. index 0 for symmetric, 1 for hmac.
        List<EncryptionDataKey> keys = encryptedKeyService.pickEncryptionKeys(0,1)
                .orElseThrow(()-> new ServiceException("No encryption key available"));
        String oldEncrypt = ud.getEmailEncrypted();
        String newEncrypt = EncryptionHelper.encryptField(dto.getEmail(), keys.get(0));
        String newHashed = HashUtil.Sha256HmacString(dto.getEmail(), keys.get(1).getEncryptedKey());
        if(oldEncrypt ==  null){
            if(scene == VerifyScene.BIND_EMAIL){
                ud.setEmailEncrypted(newEncrypt);
                ud.setEmailHash(HashUtil.Sha256HmacString(dto.getEmail(), keys.get(1).getEncryptedKey()));
                ud.setEmailVerified(true);
            }else{
                throw new BusinessException(BaseResponseCode.INVALID_VERIFY_SCENE);
            }
        }else {
            if (scene == VerifyScene.ACTIVATE_EMAIL) { // only activate email
                if (ud.getEmailHash().equals(newHashed)) {
                        ud.setEmailVerified(true);
                } else {
                    throw new BusinessException(BaseResponseCode.EMAIL_INVALID);
                }
            }else{
                throw new BusinessException(BaseResponseCode.INVALID_VERIFY_SCENE);
            }
        }
        userDatumRepo.save(ud);
    }

    @Override
    public void changeEmail(long userId, String verifyCodeKey, EmailChangeDto dto) {

    }
}
