package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.BaseResponseCode;

public class TagLimitExceededException extends BizException{
    public TagLimitExceededException() {
        super(BaseResponseCode.USER_TAG_QUOTA_EXCEEDED);
    }
}
