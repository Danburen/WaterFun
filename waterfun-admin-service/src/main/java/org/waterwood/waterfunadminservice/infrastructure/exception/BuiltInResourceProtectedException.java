package org.waterwood.waterfunadminservice.infrastructure.exception;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.BizException;

public class BuiltInResourceProtectedException extends BizException {
    public BuiltInResourceProtectedException(String type) {
        super(BaseResponseCode.BUILT_IN_RESOURCE_PROTECTED, type);
    }
}
