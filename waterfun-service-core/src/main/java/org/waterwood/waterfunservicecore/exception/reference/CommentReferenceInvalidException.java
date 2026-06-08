package org.waterwood.waterfunservicecore.exception.reference;

import org.waterwood.api.BaseResponseCode;

import java.io.Serializable;

public class CommentReferenceInvalidException extends ReferenceInvalidException{
    public CommentReferenceInvalidException(Serializable reference) {
        super(BaseResponseCode.COMMENT_NOT_FOUND_ARGS, reference);
    }
}
