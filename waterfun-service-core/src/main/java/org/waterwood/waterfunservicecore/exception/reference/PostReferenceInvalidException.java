package org.waterwood.waterfunservicecore.exception.reference;

import org.waterwood.api.BaseResponseCode;

import java.io.Serializable;

public class PostReferenceInvalidException extends ReferenceInvalidException{
    public PostReferenceInvalidException(Serializable reference) {
        super(BaseResponseCode.POST_NOT_FOUND_ARGS, reference);
    }
}
