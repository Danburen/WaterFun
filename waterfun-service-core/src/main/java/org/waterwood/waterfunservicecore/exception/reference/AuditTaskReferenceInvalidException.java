package org.waterwood.waterfunservicecore.exception.reference;

import org.waterwood.api.BaseResponseCode;

import java.io.Serializable;

public class AuditTaskReferenceInvalidException extends ReferenceInvalidException{
    public AuditTaskReferenceInvalidException(Serializable reference) {
        super(BaseResponseCode.AUDIT_TASK_NOT_FOUND_ARGS, reference);
    }
}
