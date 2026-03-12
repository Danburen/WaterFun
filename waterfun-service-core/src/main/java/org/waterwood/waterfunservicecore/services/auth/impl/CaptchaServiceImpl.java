package org.waterwood.waterfunservicecore.services.auth.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.waterwood.common.cache.RedisHelperHolder;
import org.waterwood.waterfunservicecore.services.auth.CaptchaService;
import org.waterwood.waterfunservicecore.services.auth.LineCaptchaResult;
import org.waterwood.waterfunservicecore.services.auth.VerifyKeyBuilder;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {
    private final RedisHelperHolder redisHelper;
    @Override
    public LineCaptchaResult generateCaptcha(){
        LineCaptcha lineCaptcha = generateLineCaptcha();
        String uuid = UUID.randomUUID().toString();
        String code = lineCaptcha.getCode();
        redisHelper.set(VerifyKeyBuilder.captcha(uuid),code, Duration.ofMinutes(2));
        return new LineCaptchaResult(uuid,lineCaptcha);
    }

    @Override
    public LineCaptcha generateLineCaptcha() {
        return CaptchaUtil.createLineCaptcha(120, 30, 4, 10);
    }

    @Override
    public boolean verifyCode(String uuid, String code){
        return redisHelper.validateAndRemove(VerifyKeyBuilder.captcha(uuid),code);
    }
}
