package org.waterwood.waterfunservicecore.exception.notfound;

import org.waterwood.api.BaseResponseCode;

public class CategoryNotFoundException extends NotFoundException {
    public CategoryNotFoundException() {
        super(BaseResponseCode.POST_CATEGORY_NOT_FOUND);
    }
}
