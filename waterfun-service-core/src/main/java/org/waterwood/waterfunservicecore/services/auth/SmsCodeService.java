package org.waterwood.waterfunservicecore.services.auth;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.waterwood.common.cache.RedisHelperInterface;
import org.waterwood.waterfunservicecore.api.resp.auth.SmsCodeResult;
import org.waterwood.waterfunservicecore.services.sms.AliyunSmsService;
import org.waterwood.common.exceptions.ServiceException;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelper;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Getter
@Service
public class SmsCodeService implements VerifyServiceBase {
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

    public SmsCodeResult sendSmsCode(String phoneNumber) {
        String code = generateVerifyCode();
        String uuid = UUID.randomUUID().toString();
        SmsCodeResult result = smsService.sendSms(phoneNumber, smsCodeTemplate,
                Map.of("code", code, "time", expireDuration));
        result.setKey(uuid);
        if(result.isSendSuccess()) {
            redisHelper.set(redisHelper.buildKeys(phoneNumber, uuid), code, Duration.ofMinutes(expireDuration));
        }else{
            throw new ServiceException("SMS Send Failed" + result.getMessage());
        }
        return result;
    }

    public boolean verifySmsCode(String phoneNumber,String uuid, String code) {
        return redisHelper.validateAndRemove(redisHelper.buildKeys(phoneNumber,uuid), code);
    }

    @Override
    public String generateVerifyCode() {
        return String.valueOf( ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
