package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.BaseResponseCode;

public class SelfFollowIsNotAllowException extends BizException{
    public SelfFollowIsNotAllowException() {
        super(BaseResponseCode.USER_FOLLOW_SELF_NOT_ALLOW);
    }
}
