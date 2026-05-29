package org.waterwood.waterfunservicecore.exception.reference;

import org.waterwood.api.BaseResponseCode;

import java.io.Serializable;

public class TagReferenceInvalidException extends ReferenceInvalidException {
    public TagReferenceInvalidException(Serializable reference) {
        super(BaseResponseCode.POST_TAG_NOT_FOUND_ARGS, reference);
    }
}
