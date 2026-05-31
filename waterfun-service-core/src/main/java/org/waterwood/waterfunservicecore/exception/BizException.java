package org.waterwood.waterfunservicecore.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.api.ResponseCode;

@Getter
public class BizException extends RuntimeException{
    private final String  errorCode;
    private final Object[] params;
    @Setter
    private int httpStatusCode = 400;
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

    public BizException(BaseResponseCode code, HttpStatus httpStatus, Object... params){
        super(code.getCode());
        this.errorCode = code.getCode();
        this.params = params;
        this.httpStatusCode = httpStatus.value();
    }

    public BizException(ResponseCode code) {
        super(code.toNoArgsResponse().getCode());
        this.errorCode = code.getCode();
        this.params = null;
    }

    public BizException(ResponseCode code, HttpStatus httpStatus) {
        super(code.toNoArgsResponse().getCode());
        this.errorCode = code.getCode();
        this.params = null;
        this.httpStatusCode = httpStatus.value();
    }

}
