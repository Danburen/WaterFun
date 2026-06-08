package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.BaseResponseCode;

public class CommentAlreadyDeletedOrNotFoundException extends BizException{
    public CommentAlreadyDeletedOrNotFoundException() {
        super(BaseResponseCode.COMMENT_ALREADY_DELETED_OR_NOT_FOUND);
    }
}
