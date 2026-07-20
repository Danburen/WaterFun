package org.waterwood.waterfunservicecore.exception.threshold;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.BizException;

public class TagLimitExceededException extends BizException {
    public TagLimitExceededException() {
        super(BaseResponseCode.USER_TAG_QUOTA_EXCEEDED);
    }
}
