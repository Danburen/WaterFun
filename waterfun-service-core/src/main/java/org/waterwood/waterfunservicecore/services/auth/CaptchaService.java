package org.waterwood.waterfunservicecore.services.auth;

import cn.hutool.captcha.LineCaptcha;

public interface CaptchaService extends VerifyServiceBase {
    LineCaptchaResult generateCaptcha();

    @Override
    LineCaptcha generateVerifyCode();

    boolean validateCaptcha(String uuid, String code);
}
