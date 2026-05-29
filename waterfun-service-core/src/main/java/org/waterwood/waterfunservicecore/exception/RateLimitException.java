package org.waterwood.waterfunservicecore.exception;

import org.apache.http.HttpStatus;
import org.waterwood.api.BaseResponseCode;

public class RateLimitException extends BizException {
    public RateLimitException() {
        super(BaseResponseCode.RATE_LIMIT_EXCEEDED, HttpStatus.SC_TOO_MANY_REQUESTS);
    }
}
