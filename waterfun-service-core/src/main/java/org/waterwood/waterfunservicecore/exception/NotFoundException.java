package org.waterwood.waterfunservicecore.exception;

import org.jetbrains.annotations.NotNull;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.BizException;

public class NotFoundException extends BizException {
    public NotFoundException(String message) {
        super(BaseResponseCode.NOT_FOUND, message);
        super.setHttpStatus(404);
    }

    public static NotFoundException of(String s) {
        return new NotFoundException(s);
    }
}
