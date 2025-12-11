package org.waterwood.waterfunservicecore.services.sms;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.waterwood.common.cache.RedisHelperInterface;
import org.waterwood.waterfunservicecore.api.VerifyChannel;
import org.waterwood.waterfunservicecore.api.VerifyScene;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;
import org.waterwood.waterfunservicecore.services.auth.code.CodeVerifier;
import org.waterwood.waterfunservicecore.services.auth.code.CodeSender;
import org.waterwood.common.exceptions.ServiceException;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelper;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Getter
@Service
public class SmsCodeService implements CodeVerifier, CodeSender {
    private final RedisHelperInterface redisHelper;
    private static final String SMS_KEY_PREFIX = "verify:sms_code";

    @Value("${expire.sms-code}")
    private Long expireDuration;
    @Value("${aliyun.sms.verify-code.template-name}")
    private String smsCodeTemplate;
    private final AliyunSmsService smsService;

    protected SmsCodeService(RedisHelper redisHelper, AliyunSmsService smsService) {
        this.redisHelper = redisHelper;
        this.smsService = smsService;
        redisHelper.setKeyPrefix(SMS_KEY_PREFIX);
    }

    @Override
    public CodeResult sendCode(String target, VerifyScene scene) {
        String code = generateVerifyCode();
        String uuid = UUID.randomUUID().toString();
        CodeResult result =
//                smsService.sendSms(phoneNumber, smsCodeTemplate,
//                Map.of("code", code, "time", expireDuration));
        new CodeResult(true, target, "test", "test", VerifyChannel.SMS , uuid);
        result.setKey(uuid);
        log.info("send result key{}, code:{}",  result.getKey(), code);
        if(result.isSendSuccess()) {
            redisHelper.set(redisHelper.buildKeys(target, scene.getValue(), uuid), code, Duration.ofMinutes(expireDuration));
        }else{
            throw new ServiceException("SMS Send Failed" + result.getMessage());
        }
        return result;
    }

    @Override
    public boolean verifyCode(String target, VerifyScene scene, String key, String code) {
        return redisHelper.validateAndRemove(redisHelper.buildKeys(target, scene.getValue(), key), code);
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
