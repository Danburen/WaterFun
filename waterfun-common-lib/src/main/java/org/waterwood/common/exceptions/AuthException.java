package org.waterwood.common.exceptions;

import lombok.Getter;
import org.waterwood.api.AuthCode;

@Getter
public class AuthException extends RuntimeException {
    private final String errorCode;
    private final Object[] params;

    public AuthException() {
        super(AuthCode.AUTHORIZE_ERROR.getCode());
        this.errorCode = AuthCode.AUTHORIZE_ERROR.getCode();
        this.params = null;
    }

    public AuthException(AuthCode code) {
        super(code.getCode());
        this.errorCode = code.getCode();
        this.params = null;
    }

}
