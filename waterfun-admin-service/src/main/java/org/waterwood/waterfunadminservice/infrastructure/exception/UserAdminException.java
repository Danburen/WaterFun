package org.waterwood.waterfunadminservice.infrastructure.exception;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.BizException;

public class UserAdminException extends BizException {

    public UserAdminException(String errorCode, String msgKey, Object[] params) {
        super(errorCode, msgKey, params);
    }

    public UserAdminException(BaseResponseCode baseResponseCode, Object... params) {
        super(baseResponseCode, params);
    }
}

