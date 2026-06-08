package org.waterwood.waterfunservicecore.exception.notfound;

import org.waterwood.api.BaseResponseCode;

public class CommentNotFoundException extends NotFoundException {
    public CommentNotFoundException() {
        super(BaseResponseCode.COMMENT_NOT_FOUND);
    }
}
