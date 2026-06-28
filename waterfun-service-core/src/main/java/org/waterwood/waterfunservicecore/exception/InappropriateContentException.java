package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.BaseResponseCode;

public class InappropriateContentException extends BizException {

    public InappropriateContentException() {
        super(BaseResponseCode.CONTENT_INAPPROPRIATE);
    }

}
