package org.waterwood.common.exceptions;

import lombok.Getter;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.api.ResponseCode;

@Getter
public class BusinessException extends RuntimeException{
    private final String MESSAGE_KEY_PREFIX = "error";
    private final int errorCode;
    private final Object[] params;
    public BusinessException(int errorCode, String msgKey, Object[] params) {
        super(msgKey);
        this.errorCode = errorCode;
        this.params = params;
    }

    public BusinessException(int errorCode, String msgKey) {
        super(msgKey);
        this.errorCode = errorCode;
        this.params = null;
    }

    public BusinessException(BaseResponseCode code, Object... params) {
        super(code.getMsgKey());
        this.errorCode = code.getCode();
        this.params = params;
    }

    public BusinessException(ResponseCode code) {
        super(code.toNoArgsResponse().getMsgKey());
        this.errorCode = code.getCode();
        this.params = null;
    }

    public String getFullMessageKey(){
        return MESSAGE_KEY_PREFIX + "." + getMessage();
    }
}
