package org.waterwood.waterfunservicecore.exception.reference;

import org.waterwood.api.BaseResponseCode;

import java.io.Serializable;

public class ResourceReferenceInvalidException extends ReferenceInvalidException {
    public ResourceReferenceInvalidException(Serializable reference) {
        super(BaseResponseCode.RESOURCE_NOT_FOUND_ARGS, reference);
    }

    public ResourceReferenceInvalidException() {
        super(BaseResponseCode.RESOURCE_NOT_FOUND_ARGS, "null");
    }
}
