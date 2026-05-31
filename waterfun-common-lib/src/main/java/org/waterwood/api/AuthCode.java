package org.waterwood.api;

import lombok.Getter;

@Getter
public enum AuthCode implements ResponseCode {
    AUTHORIZE_ERROR("auth.error"),
    TOKEN_EXPIRED("auth.token_expired"),
    TOKEN_INVALID("auth.token_invalid"),
    TOKEN_MISSING("auth.token_missing"),
    USER_NOT_FOUND("user.not_found"),
    REAUTHORIZATION_REQUIRED("auth.reauthorization.required"),
    CAPTCHA_INVALID("auth.captcha.incorrect"),
    USERNAME_OR_PASSWORD_INCORRECT("auth.credentials_incorrect"),
    VERIFY_TARGET_UNSUPPORTED("auth.target.unsupported"),
    INVALID_VERIFY_SCENE("auth.invalid.scene"),;


    private final String code;

    AuthCode(String code) {
        this.code = code;
    }

    @Override
    public ResponseCode toNoArgsResponse() {
        return null;
    }

}
