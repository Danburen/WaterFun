package org.waterwood.waterfunadminservice.infrastructure.exception;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.BizException;

public class RoleException extends BizException {

    public RoleException(String errorCode, String msgKey, Object[] params) {
        super(errorCode, msgKey, params);
    }

    public RoleException(BaseResponseCode baseResponseCode, int id) {
        super(baseResponseCode, id);
    }
}
