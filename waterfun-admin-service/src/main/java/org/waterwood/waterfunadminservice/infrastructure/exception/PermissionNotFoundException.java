package org.waterwood.waterfunadminservice.infrastructure.exception;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.BizException;

public class PermissionNotFoundException extends BizException {
    public PermissionNotFoundException() {
        super(BaseResponseCode.PERMISSION_NOT_FOUND);
    }
}
