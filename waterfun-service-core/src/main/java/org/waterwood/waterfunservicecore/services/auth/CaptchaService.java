package org.waterwood.waterfunservicecore.services.auth;

import cn.hutool.captcha.LineCaptcha;
import org.waterwood.waterfunservicecore.services.auth.code.CodeVerifier;

public interface CaptchaService {
    LineCaptchaResult generateCaptcha();

    LineCaptcha generateVerifyCode();

    boolean verifyCode(String uuid, String code);
}
