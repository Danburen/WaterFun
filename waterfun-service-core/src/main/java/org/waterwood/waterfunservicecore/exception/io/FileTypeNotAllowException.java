package org.waterwood.waterfunservicecore.exception.io;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.BizException;

public class FileTypeNotAllowException extends BizException {
    public FileTypeNotAllowException() {
        super(BaseResponseCode.FILE_TYPE_NOT_ALLOW);
    }

    public FileTypeNotAllowException(Object... args){
        super(BaseResponseCode.FILE_TYPE_NOT_ALLOW_ARGS, args);
    }
}
