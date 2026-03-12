package org.waterwood.waterfunservicecore.services.sms;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.waterwood.common.cache.RedisHelperHolder;
import org.waterwood.common.cache.RedisKeyBuilder;
import org.waterwood.waterfunservicecore.api.VerifyChannel;
import org.waterwood.waterfunservicecore.api.VerifyScene;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;
import org.waterwood.waterfunservicecore.services.auth.VerifyKeyBuilder;
import org.waterwood.waterfunservicecore.services.auth.code.CodeVerifier;
import org.waterwood.waterfunservicecore.services.auth.code.CodeSender;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class SmsCodeService implements CodeVerifier, CodeSender {
    private final RedisHelperHolder redisHelper;

    @Value("${expire.sms-code}")
    private Long expireDuration;
    @Value("${aliyun.sms.verify-code.template-name}")
    private String smsCodeTemplate;
    private final AliyunSmsService smsService;

    @Override
    public CodeResult sendCode(String target, VerifyScene scene) {
        String code = generateVerifyCode();
        String uuid = UUID.randomUUID().toString();
        CodeResult result =
//                smsService.sendSms(phoneNumber, smsCodeTemplate,
//                Map.of("code", code, "time", expireDuration));
        new CodeResult(true, target,  VerifyChannel.SMS , uuid);
        result.setKey(uuid);
        log.info("send result key{}, code:{}",  result.getKey(), code);
        if(result.isSendSuccess()) {
            redisHelper.set(
                    RedisKeyBuilder.buildKey(
                            VerifyKeyBuilder.sms(target),
                            scene.getValue(),
                            uuid
                    ),
                    code,
                    Duration.ofMinutes(expireDuration)
            );
        }
        return result;
    }

    @Override
    public boolean verifyCode(String target, VerifyScene scene, String key, String code) {
        return redisHelper.validateAndRemove(
                RedisKeyBuilder.buildKey(
                        VerifyKeyBuilder.sms(target),
                        scene.getValue(),
                        key
                ),
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
