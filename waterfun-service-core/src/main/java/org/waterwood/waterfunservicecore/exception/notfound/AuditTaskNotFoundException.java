package org.waterwood.waterfunservicecore.exception.notfound;

import org.waterwood.api.BaseResponseCode;

public class AuditTaskNotFoundException extends NotFoundException {
    public AuditTaskNotFoundException() {
        super(BaseResponseCode.AUDIT_TASK_NOT_FOUND);
    }
}
