package org.waterwood.waterfunservicecore.services.sms;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.waterwood.utils.MaskUtil;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelperHolder;
import org.waterwood.common.cache.RedisKeyBuilder;
import static org.waterwood.common.RedisKeyPrefix.VERIFY;
import org.waterwood.waterfunservicecore.api.auth.VerifyChannel;
import org.waterwood.waterfunservicecore.api.auth.VerifyScene;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.auth.code.CodeVerifier;
import org.waterwood.waterfunservicecore.services.auth.code.CodeSender;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class SmsCodeService implements CodeVerifier, CodeSender {
    private final RedisHelperHolder redisHelper;

    @Value("${expiresIn.sms-code}")
    private Long expireDuration;
    @Value("${aliyun.sms.verify-code.template-name}")
    private String smsCodeTemplate;
    private final AliyunSmsService smsService;

    // -- Redis key builders --

    private static String smsCodeKey(String target, String scene, String identifier) {
        return RedisKeyBuilder.build(VERIFY, "sms", target, scene, identifier);
    }

    @Override
    public CodeResult sendCode(String target, VerifyScene scene) {
        String code = generateVerifyCode();
        String uuid = UUID.randomUUID().toString();
        CodeResult result =
                smsService.sendSms(target, smsCodeTemplate, Map.of("code", code, "time", expireDuration));
        result.setKey(uuid);
        log.info("Sms send to phone {} user {} send result key{}, code:{}",
                MaskUtil.maskPhone(target),
                UserCtxHolder.safeGetUserId().map(String::valueOf).orElse("anonymous"),
                result.getKey(),
                code
        );
        if(result.isSendSuccess()) {
            redisHelper.set(
                    smsCodeKey(target, scene.getValue(), uuid),
                    code,
                    Duration.ofMinutes(expireDuration)
            );
        }
        return result;
    }

    @Override
    public boolean verifyCode(String target, VerifyScene scene, String key, String code) {
        return redisHelper.validateAndRemove(
                smsCodeKey(target, scene.getValue(), key),
                code
        );
    }

    @Override
    public VerifyChannel channel() {
        return VerifyChannel.SMS;
    }

    @Override
    public String generateVerifyCode() {
        return String.valueOf( ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
