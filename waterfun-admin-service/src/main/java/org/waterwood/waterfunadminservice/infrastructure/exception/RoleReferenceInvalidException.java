package org.waterwood.waterfunadminservice.infrastructure.exception;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.BizException;

import java.io.Serializable;

public class RoleReferenceInvalidException extends BizException {
    public RoleReferenceInvalidException(Serializable reference) {
        super(BaseResponseCode.ROLE_NOT_FOUND_WITH_ARGS, reference);
    }
}
