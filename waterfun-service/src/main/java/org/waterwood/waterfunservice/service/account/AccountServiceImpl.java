package org.waterwood.waterfunservice.service.account;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.BizException;
import org.waterwood.waterfunservice.api.request.*;
import org.waterwood.waterfunservicecore.api.auth.VerifyChannel;
import org.waterwood.waterfunservicecore.api.auth.VerifyScene;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserDatum;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserDatumRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptedKeyService;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionDataKey;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionHelper;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.auth.code.VerificationService;
import org.waterwood.waterfunservicecore.services.email.ResendEmailService;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;
import org.waterwood.waterfunservicecore.services.user.UserDatumCoreService;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final VerificationService verificationService;
    private final UserDatumRepo userDatumRepo;
    private final EncryptedKeyService encryptedKeyService;
    private final UserCoreService userCoreService;
    private final UserDatumCoreService userDatumCoreService;
    private final ResendEmailService emailService;
    private final MessageSource messageSource;

    @Value("${mail.email-verify}")
    private String emailVerifyUrl;
    @Value("${expire.email.verify}")
    private Long expireDuration;
    @Override
    public void changePwd(String verifyCodeKey, ResetPasswordDto dto) {
        long userUid = UserCtxHolder.getUserUid();
        verificationService.verifyAuthorizedCode(
                verifyCodeKey,
                dto.getVerify(),
                getTargetOfChannel(dto.getVerify().getChannel()),
                VerifyScene.RESET_PASSWORD
        );
        User user = userCoreService.getUser(userUid);
        if(! dto.getConfirmPwd().equals(dto.getNewPwd())){
            throw new BizException(BaseResponseCode.PASSWORD_TWO_PASSWORD_NOT_EQUAL);
        }
        if(! encoder.matches(dto.getOldPwd(), user.getPasswordHash())){
            throw new BizException(BaseResponseCode.OLD_PASSWORD_INCORRECT);
        }
        userCoreService.changePwd(userUid, dto.getNewPwd());
    }

    @Override
    public void setPassword(String verifyCodeKey, SetPasswordDto dto) {
        long userUid = UserCtxHolder.getUserUid();
        verificationService.verifyAuthorizedCode(
                verifyCodeKey,
                dto.getVerify(), getTargetOfChannel(dto.getVerify().getChannel()),
                VerifyScene.SET_PASSWORD
        );
        User user = userRepository.findById(userUid).orElseThrow(() -> new BizException(BaseResponseCode.USER_NOT_FOUND));
        if(! dto.getConfirmPwd().equals(dto.getNewPwd())){
            throw new BizException(BaseResponseCode.PASSWORD_TWO_PASSWORD_NOT_EQUAL);
        }
        if(user.getPasswordHash() != null) {
            throw new BizException(BaseResponseCode.PASSWORD_ALREADY_SET);
        }
        user.setPasswordHash(encoder.encode(dto.getNewPwd()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void activateEmail(String verifyCodeKey, EmailBindActivateDto dto) {
        long userUid = UserCtxHolder.getUserUid();
        verificationService.verifyAuthorizedCodeWithChannel(
                verifyCodeKey,
                dto.getVerify(),
                getTargetOfChannel(dto.getVerify().getChannel()),
                VerifyScene.ACTIVATE,
                VerifyChannel.EMAIL
        );
        UserDatum ud = userDatumCoreService.saveNewEmail(userUid, dto.getEmail(), true);
        userDatumRepo.save(ud);
    }

    @Override
    @Transactional
    public CodeResult changeEmail(String verifyCodeKey, EmailChangeDto dto) {
        long userUid = UserCtxHolder.getUserUid();
        // Check scene and verify code
        VerifyChannel channel = dto.getVerify().getChannel();
        verificationService.verifyAuthorizedCode(
                verifyCodeKey,
                dto.getVerify(),
                getTargetOfChannel(channel),
                VerifyScene.CHANGE_EMAIL
        );
        // TODO: ADD MOVE VERIFICATION FOR EMAIL CHANGE AND AUDIT LOG
        return verificationService.sendAuthenticationCode(
                dto.getEmail() , VerifyChannel.EMAIL, VerifyScene.ACTIVATE);
    }

    @Override
    @Transactional
    public CodeResult bindEmail(String verifyCodeKey, EmailBindActivateDto dto) {
        long userUid = UserCtxHolder.getUserUid();
        verificationService.verifyAuthorizedCode(
                verifyCodeKey,
                dto.getVerify(),
                getTargetOfChannel(dto.getVerify().getChannel()),
                VerifyScene.BIND_EMAIL
        );
        // TODO: ADD MOVE VERIFICATION FOR EMAIL BIND AND AUDIT LOG
        return verificationService.sendAuthenticationCode(
                dto.getEmail(), VerifyChannel.EMAIL, VerifyScene.ACTIVATE);
    }

    @Override
    @Async
    @Transactional
    public void cleanUnverifiedEmail() {
        List<UserDatum> list = userDatumRepo.findUserDatumByEmailVerifiedFalse();
        list.forEach(ud->{
            if(ud.getEmailExpireAt() != null && ud.getEmailExpireAt().isBefore(Instant.now())){
                ud.setEmailEncrypted(null);
                ud.setEmailHash(null);
            }
        });
    }

    @Override
    public CodeResult changePhone(String channelVerifyCodeKey, PhoneChangeActivateDto dto) {
        long userUid = UserCtxHolder.getUserUid();
        verificationService.verifyAuthorizedCodeWithChannel(
                channelVerifyCodeKey,
                dto.getVerify(),
                getTargetOfChannel(dto.getVerify().getChannel()),
                VerifyScene.CHANGE_PHONE,
                VerifyChannel.SMS
        );
        return verificationService.sendAuthenticationCode(
                dto.getPhone(), VerifyChannel.SMS, VerifyScene.ACTIVATE);
    }

    /**
     * Activate phone forced to bind phone while not changing old phone in db.
     * so we use the request body's phone as phone number target.
     *
     * @param verifyCodeKey cached verify code key
     * @param dto           change phone number dto
     */
    @Override
    public void activatePhone(String verifyCodeKey, PhoneChangeActivateDto dto) {
        long userUid = UserCtxHolder.getUserUid();
        verificationService.verifyAuthorizedCodeWithChannel(
                verifyCodeKey,
                dto.getVerify(),
                dto.getPhone(),
                VerifyScene.ACTIVATE,
                VerifyChannel.SMS
        );
        //TODO: ADD MOVE VERIFICATION FOR PHONE CHANGE AND AUDIT LOG
        userDatumCoreService.saveNewPhone(userUid, dto.getPhone(), true);
    }

    @Override
    public void unbindEmail(String channelVerifyCodeKey, EmailBindActivateDto dto) {
        long userUid = UserCtxHolder.getUserUid();
        verificationService.verifyAuthorizedCodeWithChannel(
                channelVerifyCodeKey,
                dto.getVerify(),
                getTargetOfChannel(dto.getVerify().getChannel()),
                VerifyScene.UNBIND,
                VerifyChannel.EMAIL
        );
        String emailRaw = userDatumCoreService.getRawEmail(userUid);
        if(emailRaw == null){
            throw new BizException(BaseResponseCode.EMAIL_NOT_FOUND);
        }

        if (!emailRaw.equals(dto.getEmail())){
            throw new BizException(BaseResponseCode.EMAIL_INVALID);
        }

        // TODO ADD AUDIO LOG AND FALLBACK
        UserDatum ud = userDatumCoreService.getUserDatum(userUid);
        ud.setEmailExpireAt(null);
        ud.setEmailEncrypted(null);
        ud.setEmailVerified(false);
        userDatumRepo.save(ud);
    }

    private @NotNull String getTargetOfChannel(VerifyChannel channel) {
        long userUid = UserCtxHolder.getUserUid();
        EncryptionDataKey aesKey = encryptedKeyService.getAesKey();
        UserDatum ud = userDatumRepo.findUserDatumByUserUid(userUid)
                .orElseThrow(() -> new BizException(BaseResponseCode.USER_NOT_FOUND));
        String target;
        if(channel == VerifyChannel.EMAIL){
            target = EncryptionHelper.decryptField(ud.getEmailEncrypted(), aesKey);
        }else{
            target = EncryptionHelper.decryptField(ud.getPhoneEncrypted(), aesKey);
        }
        return target;
    }
}
