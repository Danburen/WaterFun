package org.waterwood.waterfunservicecore.infrastructure.utils.io;

import org.waterwood.api.BaseResponseCode;

public class ResponseCodeMapper {
    public static BaseResponseCode toNoArgsResponseCode(BaseResponseCode baseResponseCode){
        return switch (baseResponseCode){
            case OK -> BaseResponseCode.OK;
            case NOT_FOUND -> BaseResponseCode.HTTP_NOT_FOUND;
            case FORBIDDEN -> BaseResponseCode.HTTP_FORBIDDEN;
            default -> baseResponseCode;
        };
    }
}
