package org.waterwood.waterfunservicecore.exception.notfound;

import org.springframework.http.HttpStatus;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.BizException;

import java.io.Serializable;

public class NotFoundException extends BizException {
    public NotFoundException(String message) {
        super(BaseResponseCode.NOT_FOUND, message);
        super.setHttpStatusCode(404);
    }

    public NotFoundException() {
        super(BaseResponseCode.HTTP_NOT_FOUND);
        super.setHttpStatusCode(404);
    }

    public NotFoundException(BaseResponseCode responseCode) {
        super(responseCode);
        super.setHttpStatusCode(404);
    }

    public NotFoundException(BaseResponseCode baseResponseCode, Serializable id) {
        super(baseResponseCode, id);
    }

    public NotFoundException(BaseResponseCode baseResponseCode, HttpStatus httpStatus, Serializable id) {
        super(baseResponseCode, httpStatus, id);
    }

    public static NotFoundException of(String s) {
        return new NotFoundException(s);
    }
}
