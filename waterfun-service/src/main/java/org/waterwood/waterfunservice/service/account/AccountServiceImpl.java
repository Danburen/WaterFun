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
import org.waterwood.waterfunservice.api.request.*;
import org.waterwood.waterfunservicecore.api.auth.VerifyChannel;
import org.waterwood.waterfunservicecore.api.auth.VerifyScene;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;
import org.waterwood.waterfunservicecore.entity.EncryptionDataKey;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserDatum;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserDatumRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptedKeyService;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionHelper;
import org.waterwood.waterfunservicecore.api.req.auth.DeviceInfoReq;
import org.waterwood.waterfunservicecore.entity.audit.AuditLogActionType;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.audit.AuditLogCoreService;
import org.waterwood.waterfunservicecore.services.auth.code.VerificationService;
import org.waterwood.waterfunservicecore.services.email.ResendEmailService;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;
import org.waterwood.waterfunservicecore.services.user.UserDatumCoreService;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final VerificationService verificationService;
    private final UserDatumRepo userDatumRepo;
    private final EncryptedKeyService encryptedKeyService;
    private final UserCoreService userCoreService;
    private final UserDatumCoreService userDatumCoreService;
    private final AuditLogCoreService auditLogCoreService;
    private final ResendEmailService emailService;
    private final MessageSource messageSource;

    @Value("${mail.email-verify}")
    private String emailVerifyUrl;
    @Value("${expire.email.verify}")
    private Long expireDuration;
    @Transactional
    @Override
    public void changePwd(String verifyCodeKey, ResetPasswordDto dto) {
        long userUid = UserCtxHolder.getUserUid();
        DeviceInfoReq deviceInfo = dto.getVerify() != null ? dto.getVerify().getDeviceInfo() : null;
        try {
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
            // Skip old-password verification for users who registered without a password (e.g., quick login)
            if(user.getPasswordHash() != null && !encoder.matches(dto.getOldPwd(), user.getPasswordHash())){
                throw new BizException(BaseResponseCode.OLD_PASSWORD_INCORRECT);
            }
            userCoreService.changePwd(userUid, dto.getNewPwd());
        } catch (Exception e) {
            auditLogCoreService.recordFailure(userUid, null, AuditLogActionType.CHANGE_PASSWORD,
                    e.getMessage(), deviceInfo);
            throw e;
        }
    }

    @Transactional
    @Override
    public void setPassword(String verifyCodeKey, SetPasswordDto dto) {
        long userUid = UserCtxHolder.getUserUid();
        DeviceInfoReq deviceInfo = dto.getVerify() != null ? dto.getVerify().getDeviceInfo() : null;
        try {
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
            auditLogCoreService.recordSuccess(userUid, user.getUsername(),
                    AuditLogActionType.CHANGE_PASSWORD, deviceInfo);
        } catch (Exception e) {
            auditLogCoreService.recordFailure(userUid, null, AuditLogActionType.CHANGE_PASSWORD,
                    e.getMessage(), deviceInfo);
            throw e;
        }
    }

    @Override
    @Transactional
    public void activateEmail(String verifyCodeKey, EmailBindActivateDto dto) {
        long userUid = UserCtxHolder.getUserUid();
        DeviceInfoReq deviceInfo = dto.getVerify() != null ? dto.getVerify().getDeviceInfo() : null;
        try {
            verificationService.verifyAuthorizedCodeWithChannel(
                    verifyCodeKey,
                    dto.getVerify(),
                    dto.getEmail(),
                    VerifyScene.ACTIVATE,
                    VerifyChannel.EMAIL
            );
            UserDatum ud = userDatumCoreService.saveNewEmail(userUid, dto.getEmail(), true);
            userDatumRepo.save(ud);
            auditLogCoreService.recordSuccess(userUid, null,
                    AuditLogActionType.BIND_EMAIL, deviceInfo);
        } catch (Exception e) {
            auditLogCoreService.recordFailure(userUid, null, AuditLogActionType.BIND_EMAIL,
                    e.getMessage(), deviceInfo);
            throw e;
        }
    }

    @Override
    @Transactional
    public CodeResult changeEmail(String verifyCodeKey, EmailChangeDto dto) {
        // Verify identity via SMS (phone is primary auth)
        verificationService.verifyAuthorizedCodeWithChannel(
                verifyCodeKey,
                dto.getVerify(),
                getTargetOfChannel(VerifyChannel.SMS),
                VerifyScene.CHANGE_EMAIL,
                VerifyChannel.SMS
        );
        // TODO: ADD MOVE VERIFICATION FOR EMAIL CHANGE AND AUDIT LOG
        return verificationService.sendCodeForAuthenticated(
                dto.getEmail() , VerifyChannel.EMAIL, VerifyScene.ACTIVATE);
    }

    @Override
    @Transactional
    public CodeResult bindEmail(String verifyCodeKey, EmailBindActivateDto dto) {
        verificationService.verifyAuthorizedCode(
                verifyCodeKey,
                dto.getVerify(),
                getTargetOfChannel(dto.getVerify().getChannel()),
                VerifyScene.BIND_EMAIL
        );
        // TODO: ADD MOVE VERIFICATION FOR EMAIL BIND AND AUDIT LOG
        return verificationService.sendCodeForAuthenticated(
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
        verificationService.verifyAuthorizedCodeWithChannel(
                channelVerifyCodeKey,
                dto.getVerify(),
                getTargetOfChannel(dto.getVerify().getChannel()),
                VerifyScene.CHANGE_PHONE,
                VerifyChannel.SMS
        );
        return verificationService.sendCodeForAuthenticated(
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
        DeviceInfoReq deviceInfo = dto.getVerify() != null ? dto.getVerify().getDeviceInfo() : null;
        try {
            verificationService.verifyAuthorizedCodeWithChannel(
                    verifyCodeKey,
                    dto.getVerify(),
                    dto.getPhone(),
                    VerifyScene.ACTIVATE,
                    VerifyChannel.SMS
            );
            userDatumCoreService.saveNewPhone(userUid, dto.getPhone(), true);
            auditLogCoreService.recordSuccess(userUid, null,
                    AuditLogActionType.CHANGE_PHONE, deviceInfo);
        } catch (Exception e) {
            auditLogCoreService.recordFailure(userUid, null, AuditLogActionType.CHANGE_PHONE,
                    e.getMessage(), deviceInfo);
            throw e;
        }
    }

    @Override
    public void unbindEmail(String channelVerifyCodeKey, EmailBindActivateDto dto) {
        long userUid = UserCtxHolder.getUserUid();
        DeviceInfoReq deviceInfo = dto.getVerify() != null ? dto.getVerify().getDeviceInfo() : null;
        try {
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

            UserDatum ud = userDatumCoreService.getUserDatum(userUid);
            ud.setEmailExpireAt(null);
            ud.setEmailEncrypted(null);
            ud.setEmailVerified(false);
            userDatumRepo.save(ud);
            auditLogCoreService.recordSuccess(userUid, null,
                    AuditLogActionType.UNBIND_EMAIL, deviceInfo);
        } catch (Exception e) {
            auditLogCoreService.recordFailure(userUid, null, AuditLogActionType.UNBIND_EMAIL,
                    e.getMessage(), deviceInfo);
            throw e;
        }
    }

    private @NotNull String getTargetOfChannel(VerifyChannel channel) {
        long userUid = UserCtxHolder.getUserUid();
        UserDatum ud = userDatumRepo.findUserDatumByUserUid(userUid)
                .orElseThrow(() -> new BizException(BaseResponseCode.USER_NOT_FOUND));
        EncryptionDataKey aesKey = encryptedKeyService.getKeyById(ud.getEncryptionKeyId());
        String target;
        if(channel == VerifyChannel.EMAIL){
            target = EncryptionHelper.decryptField(ud.getEmailEncrypted(), aesKey);
        }else{
            target = EncryptionHelper.decryptField(ud.getPhoneEncrypted(), aesKey);
        }
        return target;
    }
}
