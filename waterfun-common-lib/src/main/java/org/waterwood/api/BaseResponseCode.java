package org.waterwood.api;

import lombok.Getter;

/**
 * An enum class to store all the response codes
 */
@Getter
public enum BaseResponseCode implements ResponseCode {
    // General HTTP Status
    SUCCESS("success"),
    OK("http.success"),
    HTTP_BAD_REQUEST("http.bad_request"),
    HTTP_UNAUTHORIZED("http.unauthorized"),
    HTTP_FORBIDDEN("http.forbidden"),
    HTTP_NOT_FOUND("http.not_found"),
    HTTP_CONFLICT("http.conflict"),
    INTERNAL_SERVER_ERROR("http.internal_server_error"),

    NOT_FOUND("general.not_found"),
    FORBIDDEN("general.forbidden"),
    CONFLICT("general.conflict"),
    // Other general
    UNKNOWN_ERROR("general.unknown_error"),
    VALIDATION_ERROR("general.validation_error"),
    RESOURCE_NOT_FOUND("general.resource_not_found"),
    PARENT_NOT_FOUND("general.parent_not_found"),

    // Validation
    USERNAME_EMPTY_OR_INVALID("user.validation.username_invalid"),
    PASSWORD_EMPTY_OR_INVALID("user.validation.password_invalid"),
    CAPTCHA_EMPTY("valid.captcha.empty"),
    PHONE_NUMBER_EMPTY_OR_INVALID("valid.phone.invalid"),
    EMAIL_ADDRESS_EMPTY_OR_INVALID("valid.email_address.invalid"),

    // Verification
    USERNAME_OR_PASSWORD_INCORRECT("user.verify.credentials_incorrect"),
    USERNAME_ALREADY_REGISTERED("general.verification.already_exists"),
    EMAIL_ALREADY_USED("general.verification.already_exists"),
    PHONE_NUMBER_ALREADY_USED("general.verification.already_exists"),
    VERIFY_CODE_INVALID("verify.code.invalid"),
    CAPTCHA_INVALID("verify.captcha.incorrect"),
    USER_ALREADY_EXISTS("user.verify.already_exists"),
    USER_NOT_FOUND("user.verify.not_found"),
    EMAIL_INVALID("email.verify.invalid"),
    EMAIL_NOT_FOUND("email.not.found"),
    VERIFY_TARGET_UNSUPPORTED("verify.target.unsupported"),
    TWO_VALUE_MUST_DIFFERENT("verify.two_value_must_diff"),



    // Role & Permissions
    ROLE_NOT_FOUND("permission.role.not_found"),
    ROLE_NOT_FOUND_WITH_ARGS("permission.role.not_found.args"),

    ROLE_ALREADY_EXISTS("permission.role.already_exists"),
    ROLE_ALREADY_EXISTS_WITH_ARGS("permission.role.already_exists.args"),

    PERMISSION_NOT_FOUND("permission.permission.not_found"),
    PERMISSION_ALREADY_EXISTS("permission.permission.already_exists"),
    PERMISSION_ALREADY_EXISTS_WITH_ARGS("permission.permission.already_exists.args"),

    // System
    REDUNDANT_OPERATION("system.redundant_operation"),
    INVALID_PATH("system.invalid_path"),
    REQUEST_NOT_IN_WHITELIST("system.request_not_in_whitelist"),
    INVALID_CONTENT_TYPE("system.invalid_content_type"),

    PASSWORD_TWO_PASSWORD_MUST_DIFFERENT("user.valid.two_pwd_must_diff"),
    PASSWORD_TWO_PASSWORD_NOT_EQUAL("user.valid.two_pwd_not_equal"),
    PASSWORD_ALREADY_SET("user.valid.pwd_already_set"),

    // Forbidden
    REAUTHENTICATE_REQUIRED("auth.reauthenticate.required"),


    DUPLICATE_ENTITY("system.duplicate_entity"),

    // Post
    POST_CATEGORY_EXISTS("post.category.exists" ),
    POST_TAG_EXISTS("post.tag.exists"),

    INVALID_VERIFY_SCENE("verify.invalid.scene"),
    CHANNEL_NOT_SUPPORT("error.channel.not.supported" ),
    OLD_PASSWORD_INCORRECT("user.verify.old_password_incorrect" ),
    NEED_FILE_TYPE("valid.file_type_required"),
    USER_ROLE_NOT_FOUND("user.role.not_found"),
    USER_PERMISSION_NOT_FOUND("user.permission.not_found");
    private final String code;
    // private final String defaultMessage;

    BaseResponseCode(String code) {
        this.code = code;
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
