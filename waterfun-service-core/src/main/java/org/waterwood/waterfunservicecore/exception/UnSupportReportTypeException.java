package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.BaseResponseCode;

public class UnSupportReportTypeException extends BizException {
    public UnSupportReportTypeException() {
        super(BaseResponseCode.UNSUPPORTED_REPORT_TYPE);
    }
}
