package org.waterwood.waterfunservicecore.exception.reference;

import org.waterwood.api.BaseResponseCode;

import java.io.Serializable;

public class CategoryReferenceInvalidException extends ReferenceInvalidException {
    public CategoryReferenceInvalidException(Serializable reference) {
        super(BaseResponseCode.POST_CATEGORY_NOT_FOUND_ARGS, reference);
    }
}
