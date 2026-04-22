package org.waterwood.common.exceptions;

import org.waterwood.api.BaseResponseCode;

public class ForbiddenException extends BizException {
    public ForbiddenException() {
        super(BaseResponseCode.FORBIDDEN);
    }
}

