package org.waterwood.waterfunservicecore.services.auth.code;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.waterfunservicecore.exception.InvalidVerifySceneException;
import org.waterwood.waterfunservicecore.exception.threshold.RateLimitException;
import org.waterwood.waterfunservicecore.exception.RegisterChannelUnsupportedException;
import org.waterwood.waterfunservicecore.api.auth.VerifyChannel;
import org.waterwood.waterfunservicecore.api.auth.VerifyScene;
import org.waterwood.waterfunservicecore.api.req.auth.SecurityVerifyCodeDto;
import org.waterwood.waterfunservicecore.api.req.auth.SendCodeReq;
import org.waterwood.waterfunservicecore.api.req.auth.VerifyCodeDto;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;
import org.waterwood.waterfunservicecore.entity.user.UserDatum;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelperHolder;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserDatumRepo;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptedKeyService;
import org.waterwood.waterfunservicecore.entity.EncryptionDataKey;
import org.waterwood.waterfunservicecore.infrastructure.security.EncryptionHelper;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.waterwood.common.RedisKeyPrefix.THRESHOLD;

import org.waterwood.common.cache.RedisKeyBuilder;
import org.waterwood.utils.codec.HashUtil;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {
    private final CodeSenderFactory codeSenderFactory;
    private final CodeVerifierFactory codeVerifierFactory;
    private final EncryptedKeyService encryptedKeyService;
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
    public CodeResult sendCodeForAuthenticated(String target, VerifyChannel channel, VerifyScene scene) {
        checkTargetNotRateLimited(target, channel);
        CodeSender sender = codeSenderFactory.of(channel);
        CodeResult result = sender.sendCode(target, scene);
        recordTargetRateLimit(target, channel);
        return result;
    }

    @Override
    public CodeResult sendCodeForAnonymous(SendCodeReq dto) {
        // White list for anonymous, only allow login, register, forgot-password
        if(! List.of(VerifyScene.LOGIN, VerifyScene.REGISTER, VerifyScene.FORGOT_PASSWORD).contains(dto.getScene())) {
            throw new InvalidVerifySceneException();
        }
        if (dto.getChannel() == VerifyChannel.EMAIL && dto.getScene() == VerifyScene.REGISTER) {
            throw new RegisterChannelUnsupportedException();
        }
        String target = dto.getTarget();
        VerifyChannel channel = dto.getChannel();
        // For LOGIN and FORGOT_PASSWORD: only send code if target is registered.
        // Return 200 either way to not reveal registration status.
        if (dto.getScene() != VerifyScene.REGISTER && !isTargetRegistered(target, channel)) {
            return CodeResult.builder()
                    .sendSuccess(true)
                    .target(target)
                    .channel(channel)
                    .key(UUID.randomUUID().toString())
                    .build();
        }

        checkTargetNotRateLimited(target, channel);
        CodeSender sender = codeSenderFactory.of(channel);
        CodeResult result = sender.sendCode(target, dto.getScene());
        recordTargetRateLimit(target, channel);
        return result;
    }

    /**
     * Check whether the given target (phone/email) is registered in the system.
     * Used by {@link #sendCodeForAnonymous} to decide whether to actually send a code.
     */
    private boolean isTargetRegistered(String target, VerifyChannel channel) {
        EncryptionDataKey hmacKey = encryptedKeyService.getUserDatumHmacKey();
        String hash = HashUtil.toSha256HmacString(target, hmacKey.getEncryptedKey());
        return switch (channel) {
            case SMS -> userDatumRepo.findByPhoneHash(hash).isPresent();
            case EMAIL -> userDatumRepo.findByEmailHash(hash).isPresent();
        };
    }
    
    /** Per-target hourly limit: max 10 sends per rolling hour (cost-sensitive channels). */
    private static final int TARGET_HOURLY_LIMIT = 10;
    /** Max failed verification attempts before temporary lockout. */
    private static final int VERIFY_MAX_ATTEMPTS = 5;
    /** Lockout duration (minutes) after exceeding max failed attempts. */
    private static final long VERIFY_LOCK_MINUTES = 15;

    // -- Redis key builders --

    /** 1-minute send-rate window key: {@code threshold:target:{channel}:{target}:1m} */
    private static String targetMinKey(String channel, String target) {
        return RedisKeyBuilder.build(THRESHOLD, "target", channel, target, "1m");
    }

    /** 1-hour send-rate window key: {@code threshold:target:{channel}:{target}:1h} */
    private static String targetHourKey(String channel, String target) {
        return RedisKeyBuilder.build(THRESHOLD, "target", channel, target, "1h");
    }

    /** Failed-verification counter key: {@code threshold:vfail:{channel}:{target}:{scene}} */
    private static String verifyFailKey(String channel, String target, String scene) {
        return RedisKeyBuilder.build(THRESHOLD, "vfail", channel, target, scene);
    }

    /**
     * Check if this target+channel combination is rate-limited (Redis hasKey).
     * Dual-window: 1/min (all channels) + 10/hour (cost-sensitive).
     * Must be paired with {@link #recordTargetRateLimit} after the actual send succeeds.
     */
    private void checkTargetNotRateLimited(String target, VerifyChannel channel) {
        String ch = channel.name();
        // 1-minute window
        if (Boolean.TRUE.equals(redisHelper.hasKey(targetMinKey(ch, target)))) {
            throw new RateLimitException();
        }

        // 1-hour window
        String hourVal = redisHelper.getValue(targetHourKey(ch, target));
        if (hourVal != null && Integer.parseInt(hourVal) >= TARGET_HOURLY_LIMIT) {
            throw new RateLimitException();
        }
    }

    /**
     * Record a rate-limit entry AFTER a successful code send, so a send failure
     * does not falsely block the user for the window duration.
     */
    private void recordTargetRateLimit(String target, VerifyChannel channel) {
        String ch = channel.name();
        // 1-minute window
        redisHelper.set(targetMinKey(ch, target), "1", Duration.ofMinutes(1));

        // 1-hour window (atomic INCR)
        redisHelper.increment(targetHourKey(ch, target), Duration.ofHours(1));
    }

    @Override
    public void verifyCode(String target, VerifyScene scene, VerifyChannel channel, String key, String code) {
        // Check brute-force lockout: too many failed attempts?
        String ch = channel.name();
        String failKey = verifyFailKey(ch, target, scene.name());
        String val = redisHelper.getValue(failKey);
        if (val != null && Integer.parseInt(val) >= VERIFY_MAX_ATTEMPTS) {
            throw new RateLimitException();
        }

        CodeVerifier verifier = codeVerifierFactory.of(channel);
        if(! verifier.verifyCode(target, scene, key, code)){
            redisHelper.increment(failKey, Duration.ofMinutes(VERIFY_LOCK_MINUTES));
            throw new BizException(BaseResponseCode.VERIFY_CODE_INVALID);
        }
        // Success — clear fail counter
        redisHelper.del(failKey);
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
            throw new InvalidVerifySceneException();
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
