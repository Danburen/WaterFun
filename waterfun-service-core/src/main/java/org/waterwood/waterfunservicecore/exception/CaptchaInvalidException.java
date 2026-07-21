package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.AuthCode;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.AuthException;

public class CaptchaInvalidException extends AuthException {
    public CaptchaInvalidException() {
        super(AuthCode.CAPTCHA_INVALID);
    }
}
