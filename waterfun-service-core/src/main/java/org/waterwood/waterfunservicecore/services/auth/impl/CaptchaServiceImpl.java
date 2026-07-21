package org.waterwood.waterfunservicecore.services.auth.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelperHolder;
import org.waterwood.waterfunservicecore.services.auth.CaptchaService;
import org.waterwood.waterfunservicecore.services.auth.LineCaptchaResult;
import org.waterwood.common.cache.RedisKeyBuilder;
import static org.waterwood.common.RedisKeyPrefix.VERIFY;

import java.time.Duration;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {
    private final RedisHelperHolder redisHelper;

    // -- Redis key builders --

    private static String captchaKey(String uuid) {
        return RedisKeyBuilder.build(VERIFY, "captcha", uuid);
    }

    @Override
    public LineCaptchaResult generateCaptcha(){
        LineCaptcha lineCaptcha = generateLineCaptcha();
        String uuid = UUID.randomUUID().toString();
        String code = lineCaptcha.getCode();
        redisHelper.set(captchaKey(uuid), code, Duration.ofMinutes(2));
        return new LineCaptchaResult(uuid,lineCaptcha);
    }

    @Override
    public LineCaptcha generateLineCaptcha() {
        return CaptchaUtil.createLineCaptcha(120, 30, 4, 10);
    }

    @Override
    public boolean verifyCode(String uuid, String code){
        return redisHelper.validateAndRemove(captchaKey(uuid), code);
    }
}
