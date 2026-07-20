package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.BaseResponseCode;

import java.time.Duration;

public class AttemptLimitExceededException extends BizException{
    public AttemptLimitExceededException(long limitMinutes) {
        super(BaseResponseCode.AttemptLimitExceededException, limitMinutes);
    }
}
