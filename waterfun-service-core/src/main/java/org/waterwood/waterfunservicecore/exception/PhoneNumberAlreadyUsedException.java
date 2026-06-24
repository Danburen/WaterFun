package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.BaseResponseCode;

public class PhoneNumberAlreadyUsedException extends BizException {
    public PhoneNumberAlreadyUsedException() {
        super(BaseResponseCode.PHONE_NUMBER_ALREADY_USED);
    }
}
