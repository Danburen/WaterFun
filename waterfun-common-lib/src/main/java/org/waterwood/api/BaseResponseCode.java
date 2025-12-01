package org.waterwood.api;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * An enum class to store all the response codes
 */
@Getter
public enum BaseResponseCode implements ResponseCode {
    // General HTTP Status
    OK(200, "http.success"),
    HTTP_BAD_REQUEST(400, "http.bad_request"),
    HTTP_UNAUTHORIZED(401, "http.unauthorized"),
    HTTP_FORBIDDEN(403, "http.forbidden"),
    HTTP_NOT_FOUND(404, "http.not_found"),
    HTTP_CONFLICT(409, "http.conflict"),
    INTERNAL_SERVER_ERROR(500, "http.internal_server_error"),

    NOT_FOUND(404, "general.not_found"),
    FORBIDDEN(403, "general.forbidden"),
    CONFLICT(409, "general.conflict"),
    // Other general
    UNKNOWN_ERROR(500000, "general.unknown_error"),
    VALIDATION_ERROR(400000, "general.validation_error"),
    RESOURCE_NOT_FOUND(404000, "general.resource_not_found"),
    PARENT_NOT_FOUND(404001, "general.parent_not_found"),

    // Validation
    USERNAME_EMPTY_OR_INVALID(400001, "user.validation.username_invalid"),
    PASSWORD_EMPTY_OR_INVALID(400002, "user.validation.password_invalid"),
    CAPTCHA_EMPTY(400012, "valid.captcha.empty"),
    PHONE_NUMBER_EMPTY_OR_INVALID(400015, "valid.phone.invalid"),
    EMAIL_ADDRESS_EMPTY_OR_INVALID(400016, "valid.email_address.invalid"),

    // Verification
    USERNAME_OR_PASSWORD_INCORRECT(400003, "user.verify.credentials_incorrect"),
    USERNAME_ALREADY_REGISTERED(400027, "general.verification.already_exists"),
    EMAIL_ALREADY_USED(400028, "general.verification.already_exists"),
    PHONE_NUMBER_ALREADY_USED(400029, "general.verification.already_exists"),

    USER_ALREADY_EXISTS(400017, "user.verify.already_exists"),
    USER_NOT_FOUND(400018, "user.verify.not_found"),

    // Verification Codes
    CAPTCHA_EXPIRED(400004, "verification.captcha.expired"),
    CAPTCHA_INCORRECT(400005, "verification.captcha.incorrect"),
    VERIFY_CODE_EXPIRED(400006, "verification.code.expired"),
    VERIFY_CODE_INCORRECT(400007, "verification.code.incorrect"),

    // SMS Verification
    SMS_CODE_EXPIRED(400008, "verification.sms.expired"),
    SMS_CODE_INCORRECT(400009, "verification.sms.incorrect"),
    SMS_CODE_EMPTY(400013, "valid.sms.empty"),

    // Email Verification
    EMAIL_CODE_EXPIRED(400010, "verification.email.expired"),
    EMAIL_CODE_INCORRECT(400011, "verification.email.incorrect"),
    EMAIL_CODE_EMPTY(400014, "verification.email.empty"),

    // Role & Permissions
    ROLE_NOT_FOUND(400019, "permission.role.not_found"),
    ROLE_NOT_FOUND_WITH_ARGS(400019, "permission.role.not_found.args"),

    ROLE_ALREADY_EXISTS(400020, "permission.role.already_exists"),
    ROLE_ALREADY_EXISTS_WITH_ARGS(400020, "permission.role.already_exists.args"),

    PERMISSION_NOT_FOUND(400021, "permission.permission.not_found"),
    PERMISSION_ALREADY_EXISTS(400022, "permission.permission.already_exists"),
    PERMISSION_ALREADY_EXISTS_WITH_ARGS(400022, "permission.permission.already_exists.args"),

    // System
    REDUNDANT_OPERATION(400023, "system.redundant_operation"),
    INVALID_PATH(400024, "system.invalid_path"),
    REQUEST_NOT_IN_WHITELIST(400025, "system.request_not_in_whitelist"),
    INVALID_CONTENT_TYPE(400026, "system.invalid_content_type"),

    PASSWORD_TWO_PASSWORD_MUST_DIFFERENT(400029, "user.valid.two_pwd_must_diff"),
    PASSWORD_TWO_PASSWORD_NOT_EQUAL(400030, "user.valid.two_pwd_not_equal"),

    // Forbidden
    REAUTHENTICATE_REQUIRED(403001, "auth.reauthenticate.required"),


    DUPLICATE_ENTITY(400031, "system.duplicate_entity"),

    // Post
    POST_CATEGORY_EXISTS(400032,"post.category.exists" ),
    POST_TAG_EXISTS(400033, "post.tag.exists");
    private final int code;    // error code
    private final String msgKey;

    BaseResponseCode(int code, String msgKey) {
        this.code = code;
        this.msgKey = msgKey;
    }

    @Override
    public ResponseCode toNoArgsResponse() {
        return switch (this){
            case OK -> BaseResponseCode.OK;
            case NOT_FOUND -> BaseResponseCode.HTTP_NOT_FOUND;
            case FORBIDDEN -> BaseResponseCode.HTTP_FORBIDDEN;
            default -> this;
        };
    }
}
