package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.BizException;

public class NotFoundException extends BizException {
    public NotFoundException(String message) {
        super(BaseResponseCode.NOT_FOUND, message);
    }
}
