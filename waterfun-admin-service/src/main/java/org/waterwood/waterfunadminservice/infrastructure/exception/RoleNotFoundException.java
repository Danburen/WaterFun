package org.waterwood.waterfunadminservice.infrastructure.exception;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.BizException;

public class RoleNotFoundException extends BizException {
    public RoleNotFoundException() {
        super(BaseResponseCode.ROLE_NOT_FOUND);
    }
}
