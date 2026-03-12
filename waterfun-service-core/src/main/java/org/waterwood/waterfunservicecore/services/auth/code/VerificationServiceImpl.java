package org.waterwood.waterfunservicecore.services.auth.code;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.BizException;
import org.waterwood.waterfunservicecore.api.VerifyChannel;
import org.waterwood.waterfunservicecore.api.VerifyScene;
import org.waterwood.waterfunservicecore.api.req.auth.SecurityVerifyCodeDto;
import org.waterwood.waterfunservicecore.api.req.auth.SendCodeDto;
import org.waterwood.waterfunservicecore.api.req.auth.VerifyCodeDto;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;
import org.waterwood.waterfunservicecore.entity.user.UserDatum;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserDatumRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptedKeyService;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionDataKey;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionHelper;
import org.waterwood.waterfunservicecore.services.sms.SmsCodeService;

import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {
    private final CodeSenderFactory codeSenderFactory;
    private final SmsCodeService smsCodeService;
    private final CodeVerifierFactory codeVerifierFactory;
    private final EncryptedKeyService encryptedKeyService;
    private final UserRepository userRepository;
    private final UserDatumRepo userDatumRepo;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public CodeResult sendAutoTargetAuthenticationCode(long userUid, VerifyChannel channel, VerifyScene scene) {
        UserDatum userDatum = userDatumRepo.findUserDatumByUserUid(userUid)
                .orElseThrow(() -> new BizException(BaseResponseCode.USER_NOT_FOUND));
        EncryptionDataKey aesKey = encryptedKeyService.getAesKey();
        CodeSender sender = codeSenderFactory.of(channel);
        String encryptedTarget = switch (channel) {
            case SMS -> userDatum.getPhoneEncrypted();
            case EMAIL -> userDatum.getEmailEncrypted();
            default -> throw new BizException(BaseResponseCode.CHANNEL_NOT_SUPPORT, channel.getValue());
        };
        return sender.sendCode(EncryptionHelper.decryptField(encryptedTarget, aesKey), scene);
    }

    @Override
    public CodeResult sendAuthenticationCode(String target, VerifyChannel channel, VerifyScene scene) {
        CodeSender sender = codeSenderFactory.of(channel);
        return sender.sendCode(target, scene);
    }

    @Override
    public CodeResult sendCode(SendCodeDto dto) {
        CodeSender sender = codeSenderFactory.of(dto.getChannel());
        return sender.sendCode(dto.getTarget(), dto.getScene());
    }

    @Override
    public void verifyCode(String target, VerifyScene scene, VerifyChannel channel, String key, String code) {
        CodeVerifier verifier = codeVerifierFactory.of(channel);
        if(! verifier.verifyCode(target, scene, key, code)){
            throw new BizException(BaseResponseCode.VERIFY_CODE_INVALID);
        }
    }

    @Override
    public void verifyCode(String verifyCodeKey, VerifyCodeDto verifyBody){
        this.verifyCode(
                verifyBody.getTarget(),
                verifyBody.getScene(),
                verifyBody.getChannel(),
                verifyCodeKey,
                verifyBody.getCode());
    }
    @Override
    public void verifyAuthorizedCode(String verifyCodeKey, SecurityVerifyCodeDto verifyBody, String target, VerifyScene scene){
        if(scene != verifyBody.getScene()){
            throw new BizException(BaseResponseCode.INVALID_VERIFY_SCENE);
        }
        this.verifyCode(
                target,
                verifyBody.getScene(),
                verifyBody.getChannel(),
                verifyCodeKey,
                verifyBody.getCode());
    }

    @Override
    public void verifyAuthorizedCodeWithChannel(String verifyCodeKey, SecurityVerifyCodeDto verifyBody, String target, VerifyScene scene, VerifyChannel... allowChannels) {
        if(! Arrays.asList(allowChannels).contains(verifyBody.getChannel())){
            throw new BizException(BaseResponseCode.CHANNEL_NOT_SUPPORT, verifyBody.getChannel());
        }
        verifyAuthorizedCode(verifyCodeKey, verifyBody, target, scene);
    }
}
