package org.waterwood.common.exceptions;

import org.waterwood.api.BaseResponseCode;

public class AuthException extends BusinessException {
    private final String MESSAGE_KEY_PREFIX = "auth";
    public AuthException(String errorCode, String msgKey) {
        super(errorCode,msgKey);
    }

    public AuthException(BaseResponseCode code) {
        super(code.getCode(), code.getCode());
    }

    public AuthException(BaseResponseCode code, Object[] params) {
        super(code.getCode(), code.getCode(), params);
    }
}
