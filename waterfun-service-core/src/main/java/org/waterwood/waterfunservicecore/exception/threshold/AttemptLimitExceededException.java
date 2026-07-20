package org.waterwood.waterfunservicecore.exception.threshold;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.BizException;

public class AttemptLimitExceededException extends BizException {
    public AttemptLimitExceededException(long limitMinutes) {
        super(BaseResponseCode.AttemptLimitExceededException, limitMinutes);
    }
}
