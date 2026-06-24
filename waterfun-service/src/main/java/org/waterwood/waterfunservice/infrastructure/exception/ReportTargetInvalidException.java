package org.waterwood.waterfunservice.infrastructure.exception;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.BizException;

public class ReportTargetInvalidException extends BizException {
    public ReportTargetInvalidException() {
        super(BaseResponseCode.REPORT_TARGET_INVALID);
    }
}
