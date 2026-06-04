package org.waterwood.waterfunservicecore.exception.conflict;

import org.springframework.http.HttpStatus;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.BizException;

public class ConflictException extends BizException {
    public ConflictException(BaseResponseCode code, Object... args) {
        super(code, HttpStatus.CONFLICT,args);
    }
}
