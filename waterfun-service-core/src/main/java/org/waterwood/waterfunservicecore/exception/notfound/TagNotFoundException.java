package org.waterwood.waterfunservicecore.exception.notfound;

import org.waterwood.api.BaseResponseCode;

public class TagNotFoundException extends NotFoundException {
    public TagNotFoundException() {
        super(BaseResponseCode.POST_TAG_NOT_FOUND);
    }

}
