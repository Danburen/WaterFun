package org.waterwood.waterfunadminservice.infrastructure.exception;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.BizException;

public class PermException extends BizException {

    public PermException(String errorCode, String msgKey, Object[] params) {
        super(errorCode, msgKey, params);
    }

    public PermException(BaseResponseCode baseResponseCode) {
        super(baseResponseCode);
    }

    public PermException(BaseResponseCode baseResponseCode, Object... params) {
        super(baseResponseCode, params);
    }
}

