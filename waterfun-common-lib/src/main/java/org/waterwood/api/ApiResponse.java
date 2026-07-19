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
    Boolean success;
    String code;
    String message;
    T data;

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, "success", "ok", null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "success", "ok", data);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, "success", message, data);
    }

    public static <T> ApiResponse<T> reject(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, data);
    }

    @Deprecated
    /*
      Using global exception holder
     */
    public static <T> ApiResponse<T> error(BaseResponseCode baseResponseCode) {
        return new ApiResponse<>(false, "error", baseResponseCode.getCode(), null);
    }
    @Deprecated
    /*
      Using global exception holder
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }

}
