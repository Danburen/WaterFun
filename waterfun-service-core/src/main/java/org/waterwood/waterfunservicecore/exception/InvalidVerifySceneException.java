package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.api.auth.VerifyScene;

public class InvalidVerifySceneException extends BizException{
    public InvalidVerifySceneException() {
        super(BaseResponseCode.INVALID_VERIFY_SCENE);
    }
}
