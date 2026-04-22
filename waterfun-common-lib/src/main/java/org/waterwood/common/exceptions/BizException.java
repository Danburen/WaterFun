package org.waterwood.common.exceptions;

import lombok.Getter;
import lombok.Setter;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.api.ResponseCode;

@Getter
public class BizException extends RuntimeException{
    private final String MESSAGE_KEY_PREFIX = "error";
    private final String  errorCode;
    private final Object[] params;
    @Setter
    private int httpStatus = 400;
    public BizException(String errorCode, String msgKey, Object[] params) {
        super(msgKey);
        this.errorCode = errorCode;
        this.params = params;
    }

    public BizException(String errorCode, String msgKey) {
        super(msgKey);
        this.errorCode = errorCode;
        this.params = null;
    }

    public BizException(BaseResponseCode code, Object... params) {
        super(code.getCode());
        this.errorCode = code.getCode();
        this.params = params;
    }

    public BizException(ResponseCode code) {
        super(code.toNoArgsResponse().getCode());
        this.errorCode = code.getCode();
        this.params = null;
    }

    public BizException(ResponseCode code, int httpStatus) {
        super(code.toNoArgsResponse().getCode());
        this.errorCode = code.getCode();
        this.params = null;
        this.httpStatus = httpStatus;
    }

    public String getFullMessageKey(){
        return MESSAGE_KEY_PREFIX + "." + getMessage();
    }

}
