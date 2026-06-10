package org.waterwood.waterfunadminservice.infrastructure.exception;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.BizException;

public class PermissionAlreadyExistException extends BizException {
    public PermissionAlreadyExistException() {
        super(BaseResponseCode.PERMISSION_ALREADY_EXISTS);
    }

    public PermissionAlreadyExistException(final String reference){
        super(BaseResponseCode.PERMISSION_ALREADY_EXISTS_ARGS, reference);
    }
}
