package org.waterwood.waterfunservicecore.exception.reference;

import org.waterwood.api.BaseResponseCode;

import java.io.Serializable;

public class AuditTaskResourceReferenceInvalid extends ReferenceInvalidException{
    public AuditTaskResourceReferenceInvalid(Serializable reference) {
        super(BaseResponseCode.AUDIT_TASK_RESOURCE_NOT_FOUND, reference);
    }
}
