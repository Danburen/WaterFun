package org.waterwood.waterfunservicecore.exception.io;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.BizException;

public class IllegalUploadArgumentException extends BizException {

    public IllegalUploadArgumentException() {
        super(BaseResponseCode.ILLEGAL_UPLOAD_FILE_ARGUMENTS);
    }

    public IllegalUploadArgumentException(BaseResponseCode code) {
        super(code);
    }

    public IllegalUploadArgumentException(BaseResponseCode code, Object... args) {
        super(code, args);
    }
}
