package org.waterwood.waterfunservicecore.exception.threshold;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.BizException;

public class DailyLimitExceededException extends BizException {
    public DailyLimitExceededException() {
        super(BaseResponseCode.DAILY_LIMIT_EXCEEDED);
    }
}
