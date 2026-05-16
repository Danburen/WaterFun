package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.BizException;

public class IllegalUploadArgumentException extends BizException {
    public IllegalUploadArgumentException(int count) {
        super(BaseResponseCode.ILLEGAL_FILE_COUNT, count);
    }
}
