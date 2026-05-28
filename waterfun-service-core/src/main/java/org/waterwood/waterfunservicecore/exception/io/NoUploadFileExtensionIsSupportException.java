package org.waterwood.waterfunservicecore.exception.io;

import org.waterwood.api.BaseResponseCode;

public class NoUploadFileExtensionIsSupportException extends IllegalUploadArgumentException {
    public NoUploadFileExtensionIsSupportException() {
        super(BaseResponseCode.ILLEGAL_UPLOAD_FILE_EXTENSION);
    }
}
