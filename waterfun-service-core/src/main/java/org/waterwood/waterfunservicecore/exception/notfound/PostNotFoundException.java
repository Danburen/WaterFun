package org.waterwood.waterfunservicecore.exception.notfound;

import org.waterwood.api.BaseResponseCode;

public class PostNotFoundException extends NotFoundException {
    public PostNotFoundException() {
        super(BaseResponseCode.POST_NOT_FOUND);
    }
}
