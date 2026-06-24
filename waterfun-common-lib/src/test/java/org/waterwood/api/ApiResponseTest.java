package org.waterwood.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApiResponse 统一响应封装测试")
public class ApiResponseTest {

    @Test
    @DisplayName("success() 无参数应返回 success=true, code=success")
    void success_noArgs_returnsOk() {
        ApiResponse<String> res = ApiResponse.success();
        assertTrue(res.getSuccess());
        assertEquals("success", res.getCode());
        assertEquals("ok", res.getMessage());
        assertNull(res.getData());
    }

    @Test
    @DisplayName("success(data) 应携带 data")
    void success_withData_returnsData() {
        ApiResponse<String> res = ApiResponse.success("hello");
        assertTrue(res.getSuccess());
        assertEquals("hello", res.getData());
    }

    @Test
    @DisplayName("success(data, message) 应携带 data 和自定义 message")
    void success_withDataAndMessage_returnsBoth() {
        ApiResponse<Integer> res = ApiResponse.success(42, "custom msg");
        assertTrue(res.getSuccess());
        assertEquals(42, res.getData());
        assertEquals("custom msg", res.getMessage());
    }

    @Test
    @DisplayName("error(code, message) 应返回错误响应")
    void error_withCodeAndMessage_returnsError() {
        ApiResponse<Void> res = ApiResponse.error("ban.forbidden", "您已被封禁");
        assertFalse(res.getSuccess());
        assertEquals("ban.forbidden", res.getCode());
        assertEquals("您已被封禁", res.getMessage());
    }

    @Test
    @DisplayName("error(BaseResponseCode) 应映射错误码")
    void error_withBaseResponseCode_mapsCorrectly() {
        ApiResponse<Void> res = ApiResponse.error(BaseResponseCode.BAN_FORBIDDEN);
        assertFalse(res.getSuccess());
        assertEquals("error", res.getCode());
        assertEquals("ban.forbidden", res.getMessage());
    }

    @Test
    @DisplayName("reject() 应返回拒绝响应并携带 data")
    void reject_returnsRejectionWithData() {
        ApiResponse<String> res = ApiResponse.reject("rate_limit", "too fast", "retry after 60s");
        assertFalse(res.getSuccess());
        assertEquals("rate_limit", res.getCode());
        assertEquals("too fast", res.getMessage());
        assertEquals("retry after 60s", res.getData());
    }
}
