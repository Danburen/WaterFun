package org.waterwood.waterfunservicecore.exception;

import org.springframework.http.HttpStatus;
import org.waterwood.api.BaseResponseCode;

public class ForbiddenException extends BizException {
    public ForbiddenException() {
        super(BaseResponseCode.FORBIDDEN, HttpStatus.FORBIDDEN);
    }
}

