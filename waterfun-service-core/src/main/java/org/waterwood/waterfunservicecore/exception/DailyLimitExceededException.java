package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.BaseResponseCode;

public class DailyLimitExceededException extends BizException{
    public DailyLimitExceededException() {
        super(BaseResponseCode.DAILY_LIMIT_EXCEEDED);
    }
}
