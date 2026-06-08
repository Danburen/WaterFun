package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.BaseResponseCode;

public class UserCollectExceedLimitException extends BizException{
    public UserCollectExceedLimitException() {
        super(BaseResponseCode.USER_COLLECT_EXCEED_LIMIT);
    }
}
