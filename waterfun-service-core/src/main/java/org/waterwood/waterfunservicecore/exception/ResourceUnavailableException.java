package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.BaseResponseCode;

import java.io.Serializable;

public class ResourceUnavailableException extends BizException{
    public ResourceUnavailableException() {
        super(BaseResponseCode.RESOURCE_UNAVAILABLE);
    }

    public ResourceUnavailableException(Serializable reference) {
        super(BaseResponseCode.RESOURCE_UNAVAILABLE_ARGS, reference);
    }
}
