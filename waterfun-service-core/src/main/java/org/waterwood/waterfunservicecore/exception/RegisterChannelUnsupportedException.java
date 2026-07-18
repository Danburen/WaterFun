package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.BaseResponseCode;

public class RegisterChannelUnsupportedException extends BizException {
    public RegisterChannelUnsupportedException() {
        super(BaseResponseCode.REGISTER_CHANNEL_UNSUPPORTED);
    }
}
