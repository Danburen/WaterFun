package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.BizException;

public class UnsupportedFileExtension extends BizException {
    public UnsupportedFileExtension(String extension, String original) {
      super(BaseResponseCode.UNSUPPORTED_FILE_EXTENSION, extension, original);
    }
}
