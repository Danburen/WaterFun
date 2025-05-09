package org.waterwood.waterfunservice.service.authServices;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunservice.repository.RedisRepository;

import java.util.UUID;

@Service
@Getter
public class CaptchaService extends VerifyCodeServiceBase{
    private final RedisRepository<String> redisRepository;
    private static final String redisKeyPrefix = "auth:captcha";

    protected CaptchaService(RedisRepository<String> redisRepository) {
        super(redisKeyPrefix,redisRepository);
        this.redisRepository = redisRepository;
    }
    @Override
    public LineCaptchaResult generateVerifyCode() {
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(120, 30, 4, 10);
        String uuid = getNewUUID();
        String code = lineCaptcha.getCode();
        saveCode(uuid,code);
        return new LineCaptchaResult(uuid,lineCaptcha);
    }

    public record LineCaptchaResult(String uuid, LineCaptcha captcha){}
}
