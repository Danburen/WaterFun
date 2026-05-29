package org.waterwood.waterfunservicecore.exception.reference;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.BizException;

import java.io.Serializable;

public class ReferenceInvalidException extends BizException {
    public ReferenceInvalidException() {
        super(BaseResponseCode.INVALID_REFERENCE);
    }

    public ReferenceInvalidException(BaseResponseCode baseResponseCode, Serializable reference) {
        super(baseResponseCode, reference);
    }
}
