package org.waterwood.waterfunservice.infrastructure.exception;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.BizException;

public class PhoneNumberAlreadyUsedException extends BizException {
    public PhoneNumberAlreadyUsedException() {
        super(BaseResponseCode.PHONE_NUMBER_ALREADY_USED);
    }
}
