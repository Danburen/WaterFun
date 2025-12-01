package org.waterwood.waterfunservicecore.services.auth;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelper;

import java.time.Duration;
import java.util.UUID;

@Service
public class CaptchaServiceImpl implements CaptchaService {
    private static final String REDIS_KEY_PREFIX = "verify:captcha";
    private final RedisHelper redisHelper;

    protected CaptchaServiceImpl(RedisHelper redisHelper) {
        this.redisHelper = redisHelper;
        redisHelper.setKeyPrefix(REDIS_KEY_PREFIX);
    }

    @Override
    public LineCaptchaResult generateCaptcha(){
        LineCaptcha lineCaptcha = generateVerifyCode();
        String uuid = UUID.randomUUID().toString();
        String code = lineCaptcha.getCode();
        redisHelper.set(uuid,code, Duration.ofMinutes(2));
        return new LineCaptchaResult(uuid,lineCaptcha);
    }

    @Override
    public LineCaptcha generateVerifyCode() {
        return CaptchaUtil.createLineCaptcha(120, 30, 4, 10);
    }

    @Override
    public boolean validateCaptcha(String uuid, String code){
        return redisHelper.validateAndRemove(uuid,code);
    }
}
