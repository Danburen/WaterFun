package org.waterwood.waterfunservicecore.exception.notfound;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.BizException;

public class ReportNotFoundException extends BizException {
    public ReportNotFoundException() {
        super(BaseResponseCode.REPORT_NOT_FOUND);
    }
}
