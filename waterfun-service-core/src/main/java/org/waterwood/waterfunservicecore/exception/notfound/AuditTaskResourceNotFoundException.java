package org.waterwood.waterfunservicecore.exception.notfound;

import org.waterwood.api.BaseResponseCode;

public class AuditTaskResourceNotFoundException extends NotFoundException {
    public AuditTaskResourceNotFoundException() {
        super(BaseResponseCode.AUDIT_TASK_RESOURCE_NOT_FOUND);
    }
}
