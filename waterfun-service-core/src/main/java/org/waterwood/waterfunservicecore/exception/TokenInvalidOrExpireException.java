package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.BaseResponseCode;

public class TokenInvalidOrExpireException extends BizException{
    public TokenInvalidOrExpireException() {
        super(BaseResponseCode.INVALID_TOKEN_OR_EXPIRED);
    }
}
