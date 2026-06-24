package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.BaseResponseCode;

public class ReportAlreadyExistException extends BizException {
    public ReportAlreadyExistException() {
        super(BaseResponseCode.REPORT_ALREADY_EXISTS);
    }
}
