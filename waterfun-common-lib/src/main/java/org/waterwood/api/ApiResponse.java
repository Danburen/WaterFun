package org.waterwood.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A class to process and store internal response to send to the next layout
 * The <b>code</b> field same with ResponseCode <b>NOT THE STATUS</b>
 * @see BaseResponseCode
 * @param <T>
 */
@Getter
@AllArgsConstructor
public class ApiResponse<T>{
    Integer code;
    String message;
    T data;
    public static <T> ApiResponse<T> ok(T data){
        return new ApiResponse<>(200,null,data);
    }
    public static ApiResponse<Void> response(BaseResponseCode baseResponseCode){
        return new ApiResponse<>(baseResponseCode.getCode(),null,null);
    }

    public static ApiResponse<Void> response(BaseResponseCode baseResponseCode, String message){
        return new ApiResponse<>(baseResponseCode.getCode(),message,null);
    }

    public static <T> ApiResponse<T> success(){
        return new ApiResponse<>(200, "http.success",null);
    }

    public static <T> ApiResponse<T> success(T data){
        return new ApiResponse<>(200, "http.success",data);
    }

}
