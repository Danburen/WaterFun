package org.waterwood.waterfunservicecore.services.auth.code;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.waterfunservicecore.exception.RateLimitException;
import org.waterwood.waterfunservicecore.exception.RegisterChannelUnsupportedException;
import org.waterwood.waterfunservicecore.api.auth.VerifyChannel;
import org.waterwood.waterfunservicecore.api.auth.VerifyScene;
import org.waterwood.waterfunservicecore.api.req.auth.SecurityVerifyCodeDto;
import org.waterwood.waterfunservicecore.api.req.auth.SendCodeDto;
import org.waterwood.waterfunservicecore.api.req.auth.VerifyCodeDto;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;
import org.waterwood.waterfunservicecore.entity.user.UserDatum;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelperHolder;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserDatumRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptedKeyService;
import org.waterwood.waterfunservicecore.entity.EncryptionDataKey;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionHelper;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.sms.SmsCodeService;

import java.time.Duration;
import java.util.Arrays;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {
    private final CodeSenderFactory codeSenderFactory;
    private final SmsCodeService smsCodeService;
    private final CodeVerifierFactory codeVerifierFactory;
    private final EncryptedKeyService encryptedKeyService;
    private final UserRepository userRepository;
    private final UserDatumRepo userDatumRepo;
    private final RedisHelperHolder redisHelper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public CodeResult sendAutoTargetAuthenticationCode(VerifyChannel channel, VerifyScene scene) {
        UserDatum userDatum = userDatumRepo.findUserDatumByUserUid(UserCtxHolder.getUserUid())
                .orElseThrow(() -> new BizException(BaseResponseCode.USER_NOT_FOUND));
        EncryptionDataKey aesKey = encryptedKeyService.getKeyById(userDatum.getEncryptionKeyId());
        CodeSender sender = codeSenderFactory.of(channel);
        String encryptedTarget = switch (channel) {
            case SMS -> userDatum.getPhoneEncrypted();
            case EMAIL -> userDatum.getEmailEncrypted();
            default -> throw new BizException(BaseResponseCode.CHANNEL_NOT_SUPPORT, channel.getValue());
        };
        String target = EncryptionHelper.decryptField(encryptedTarget, aesKey);
        checkTargetNotRateLimited(target, channel);
        CodeResult result = sender.sendCode(target, scene);
        recordTargetRateLimit(target, channel);
        return result;
    }

    @Override
    public CodeResult sendAuthenticationCode(String target, VerifyChannel channel, VerifyScene scene) {
        if (channel == VerifyChannel.EMAIL && scene == VerifyScene.REGISTER) {
            throw new RegisterChannelUnsupportedException();
        }
        checkTargetNotRateLimited(target, channel);
        CodeSender sender = codeSenderFactory.of(channel);
        CodeResult result = sender.sendCode(target, scene);
        recordTargetRateLimit(target, channel);
        return result;
    }

    @Override
    public CodeResult sendCode(SendCodeDto dto) {
        if (dto.getChannel() == VerifyChannel.EMAIL && dto.getScene() == VerifyScene.REGISTER) {
            throw new RegisterChannelUnsupportedException();
        }
        String target = dto.getTarget();
        VerifyChannel channel = dto.getChannel();
        checkTargetNotRateLimited(target, channel);
        CodeSender sender = codeSenderFactory.of(channel);
        CodeResult result = sender.sendCode(target, dto.getScene());
        recordTargetRateLimit(target, channel);
        return result;
    }

    /**
     * Check if this target+channel combination is rate-limited (Redis hasKey).
     * Must be paired with {@link #recordTargetRateLimit} after the actual send succeeds.
     */
    private void checkTargetNotRateLimited(String target, VerifyChannel channel) {
        String redisKey = "rate:target:" + channel.name() + ":" + target;
        if (Boolean.TRUE.equals(redisHelper.hasKey(redisKey))) {
            throw new RateLimitException();
        }
    }

    /**
     * Record a rate-limit entry AFTER a successful code send, so a send failure
     * does not falsely block the user for the window duration.
     */
    private void recordTargetRateLimit(String target, VerifyChannel channel) {
        String redisKey = "rate:target:" + channel.name() + ":" + target;
        redisHelper.set(redisKey, "1", Duration.ofMinutes(1));
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
