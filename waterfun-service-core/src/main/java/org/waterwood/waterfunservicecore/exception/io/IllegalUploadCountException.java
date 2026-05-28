package org.waterwood.waterfunservicecore.exception.io;

import org.waterwood.api.BaseResponseCode;

public class IllegalUploadCountException extends IllegalUploadArgumentException{
    public IllegalUploadCountException(int count) {
        super(BaseResponseCode.ILLEGAL_FILE_COUNT, count);
    }
}
