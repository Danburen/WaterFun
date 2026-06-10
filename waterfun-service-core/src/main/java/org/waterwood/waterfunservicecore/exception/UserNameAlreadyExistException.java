package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.BaseResponseCode;

public class UserNameAlreadyExistException extends BizException{
    public UserNameAlreadyExistException() {
        super(BaseResponseCode.USERNAME_ALREADY_REGISTERED);
    }
}
