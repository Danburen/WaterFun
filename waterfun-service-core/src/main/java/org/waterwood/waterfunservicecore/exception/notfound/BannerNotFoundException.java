package org.waterwood.waterfunservicecore.exception.notfound;

import org.waterwood.api.BaseResponseCode;

public class BannerNotFoundException extends NotFoundException {
    public BannerNotFoundException() {
        super(BaseResponseCode.BANNER_NOT_FOUND);
    }
}
