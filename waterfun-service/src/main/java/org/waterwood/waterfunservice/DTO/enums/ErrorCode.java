package org.waterwood.waterfunservice.DTO.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // general code
    SUCCESS(200, "OK"),
    USERNAME_EMPTY(40001, "USERNAME_EMPTY"),
    PASSWORD_EMPTY(40002, "PASSWORD_EMPTY"),
    USERNAME_OR_PASSWORD_INCORRECT(40003, "USERNAME_OR_PASSWORD_INCORRECT"),
    CAPTCHA_EMPTY(40004, "CAPTCHA_EMPTY"),
    CAPTCHA_INVALID(40005, "CAPTCHA_INVALID"),
    CAPTCHA_EXPIRED(40006,"CAPTCHA_EXPIRED"),
    CAPTCHA_INCORRECT(40007, "CAPTCHA_INCORRECT"),;

    private final int code;    // error code
    private final String msg; // error message

    public ResponseEntity<?> toResponseEntity() {
        Map<String,Object> body = new HashMap<>();
        body.put("code", this.code);
        body.put("message", this.msg);
        return ResponseEntity.badRequest().body(body);
    }
}
