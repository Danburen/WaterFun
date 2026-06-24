package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.BaseResponseCode;

public class ReportNotFoundException extends BizException {
    public ReportNotFoundException() {
        super(BaseResponseCode.REPORT_NOT_FOUND);
    }
}
