package org.waterwood.common.exceptions;

import org.waterwood.api.BaseResponseCode;

public class RateLimitException extends BizException {
    public RateLimitException() {
        super(BaseResponseCode.RATE_LIMIT_EXCEEDED, 429);
    }
}
