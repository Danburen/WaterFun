package org.waterwood.waterfunservicecore.exception.io;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.BizException;

public class FilePathInvalidException extends BizException {
    public FilePathInvalidException() {
        super(BaseResponseCode.INVALID_PATH);
    }
}
