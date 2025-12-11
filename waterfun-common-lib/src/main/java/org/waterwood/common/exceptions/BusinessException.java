package org.waterwood.common.exceptions;

import lombok.Getter;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.api.ResponseCode;

@Getter
public class BusinessException extends RuntimeException{
    private final String MESSAGE_KEY_PREFIX = "error";
    private final String  errorCode;
    private final Object[] params;
    public BusinessException(String errorCode, String msgKey, Object[] params) {
        super(msgKey);
        this.errorCode = errorCode;
        this.params = params;
    }

    public BusinessException(String errorCode, String msgKey) {
        super(msgKey);
        this.errorCode = errorCode;
        this.params = null;
    }

    public BusinessException(BaseResponseCode code, Object... params) {
        super(code.getCode());
        this.errorCode = code.getCode();
        this.params = params;
    }

    public BusinessException(ResponseCode code) {
        super(code.toNoArgsResponse().getCode());
        this.errorCode = code.getCode();
        this.params = null;
    }

    public String getFullMessageKey(){
        return MESSAGE_KEY_PREFIX + "." + getMessage();
    }
}
