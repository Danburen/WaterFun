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

    INTERNAL_VALIDATION_LEAK("system.internal_validation_leak"),

    NOT_FOUND("general.not_found"),
    FORBIDDEN("general.forbidden"),
    CONFLICT("general.conflict"),
    // Other general
    UNKNOWN_ERROR("general.unknown_error"),
    VALIDATION_ERROR("general.validation_error"),
    PARENT_NOT_FOUND("general.parent_not_found"),
    PARENT_MUST_DIFFERENT("general.parent_must_diff"),
    // Validation
    USERNAME_EMPTY_OR_INVALID("user.validation.username_invalid"),
    PASSWORD_EMPTY_OR_INVALID("user.validation.password_invalid"),
    CAPTCHA_EMPTY("valid.captcha.empty"),
    PHONE_NUMBER_EMPTY_OR_INVALID("valid.phone.invalid"),
    EMAIL_ADDRESS_EMPTY_OR_INVALID("valid.email_address.invalid"),

    // Verification
    USERNAME_ALREADY_REGISTERED("general.verification.already_exists"),
    EMAIL_ALREADY_USED("general.verification.already_exists"),
    PHONE_NUMBER_ALREADY_USED("general.verification.already_exists"),
    VERIFY_CODE_INVALID("verify.code.invalid"),

    USER_ALREADY_EXISTS("user.verify.already_exists"),
    USER_NOT_FOUND("user.not_found"),
    USER_NOT_FOUND_ARGS("user.not_found.args"),
    USER_TAG_QUOTA_EXCEEDED("user.tag_quota_exceeded"),
    USER_ASSOCIATION_DATA_NOT_FOUND("user.association.data.not_found"),
    USER_ASSOCIATION_DATA_NOT_FOUND_ARGS("user.association.data.not_found.args"),
    USER_COLLECT_EXCEED_LIMIT("user.collect_exceed_limit"),
    USER_FOLLOW_SELF_NOT_ALLOW("user.follow_self_not_allow"),

    EMAIL_INVALID("email.verify.invalid"),
    EMAIL_NOT_FOUND("email.not.found"),
    TWO_VALUE_MUST_DIFFERENT("verify.two_value_must_diff"),

    AUDIT_TASK_NOT_FOUND("audit.task.not_found"),
    AUDIT_TASK_NOT_FOUND_ARGS("audit.task.not_found.args"),
    AUDIT_TASK_RESOURCE_NOT_FOUND("audit.task.resource_not_found"),
    AUDIT_TASK_RESOURCE_NOT_FOUND_ARGS("audit.task.resource_not_found.args"),
    AUDIT_TASK_RESOURCE_REJECT_OR_SUSPECT("audit_task_resource_reject_or_suspect"),

    // Role & Permissions
    ROLE_NOT_FOUND("admin.role.not_found"),
    ROLE_NOT_FOUND_WITH_ARGS("admin.role.not_found.args"),
    ROLE_ALREADY_EXISTS("admin.role.already_exists"),
    ROLE_ALREADY_EXISTS_WITH_ARGS("admin.role.already_exists.args"),
    PERMISSION_NOT_FOUND("admin.permission.not_found"),
    PERMISSION_NOT_FOUND_ARGS("admin.permission.not_found.args"),
    PERMISSION_ALREADY_EXISTS("admin.permission.already_exists"),
    PERMISSION_ALREADY_EXISTS_ARGS("admin.permission.already_exists.args"),
    BUILT_IN_RESOURCE_PROTECTED("admin.system.built_in_resource_protected"),
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
    POST_NOT_FOUND("post.not_found"),
    POST_NOT_FOUND_ARGS("post.not_found.args"),
    POST_CATEGORY_EXISTS("post.category.exists" ),
    POST_TAG_EXISTS("post.tag.exists"),
    POST_CATEGORY_NOT_FOUND("post.category.not_found"),
    POST_CATEGORY_NOT_FOUND_ARGS("post.category.not_found.args"),
    POST_TAG_NOT_FOUND("post.tag.not_found"),
    POST_TAG_NOT_FOUND_ARGS("post.tag.not_found.args"),
    POST_TAG_CONFLICT("post.tag.conflict"),
    COMMENT_NOT_FOUND("comment.not_found"),
    COMMENT_NOT_FOUND_ARGS("comment.not_found.args"),
    COMMENT_ALREADY_DELETED_OR_NOT_FOUND("comment.already_deleted_or_not_found"),
    COMMENT_POST_MISMATCH("comment.post_mismatch"),

    RESOURCE_NOT_FOUND("resource_not_found"),
    RESOURCE_NOT_FOUND_ARGS("resource_not_found.args"),
    RESOURCE_UNAVAILABLE("resource_unavailable"),
    RESOURCE_UNAVAILABLE_ARGS("resource_unavailable.args"),

    INVALID_VERIFY_SCENE("verify.invalid.scene"),
    CHANNEL_NOT_SUPPORT("error.channel.not.supported" ),
    OLD_PASSWORD_INCORRECT("user.verify.old_password_incorrect" ),
    NEED_FILE_TYPE("valid.file_type_required"),
    USER_ROLE_NOT_FOUND("user.role.not_found"),
    USER_PERMISSION_NOT_FOUND("user.permission.not_found"),
    CAN_NOT_DELETE_SUPER_ADMIN_USER("system.cannot_delete_super_admin_user"),
    CAN_NOT_DELETE_SYS_ROLE("system.cannot_delete_sys_role"),
    CAN_NOT_UPDATE_SYS_ROLE_CODE("system.cannot_update_sys_role_code"),
    CAN_NOT_UNSET_SYS_ROLE("system.cannot_unset_sys_role"),
    RATE_LIMIT_EXCEEDED("system.rate_limit_exceeded"),
    INVALID_TOKEN_OR_EXPIRED("system.invalid_token_or_expired"),
    INVALID_KEY("system.invalid_key"),
    FILE_TYPE_NOT_ALLOW("system.file_type_not_allowed"),
    FILE_TYPE_NOT_ALLOW_ARGS("system.file_type_not_allowed_args"),
    COS_UPLOAD_CLIENT_NOT_CONFIGURED("system.cos_upload_client_not_configured"),
    CLOUD_FILE_NOT_FOUND("system.cloud_file_not_found"),

    ILLEGAL_FILE_COUNT("system.illegal_upload_file_count"),
    UNSUPPORTED_FILE_EXTENSION("system.unsupported_file_extension"),
    CLOUD_TOKEN_INVALID_OR_EXPIRED("system.cloud_token_invalid_or_expired"),
    UNSUPPORTED_ID_TYPE("system.unsupported_id_type"),
    ILLEGAL_UPLOAD_FILE_EXTENSION("system.illegal_upload_file_extension"),
    ILLEGAL_UPLOAD_FILE_ARGUMENTS("system.illegal_upload_file_arguments"),
    INVALID_REFERENCE("system.invalid_reference"),
    BIZ_TYPE_NOT_ALLOW_ARGS("system.biz_type_not_allow_args"),
    BANNER_NOT_FOUND("system.banner.not_found"),
    BAN_FORBIDDEN("ban.forbidden"),
    REPORT_ALREADY_EXISTS("report.already_exists"),
    REPORT_NOT_FOUND("report.not_found"),
    UNSUPPORTED_REPORT_TYPE("report.type_not_supported"),
    TicketNotFoundException("ticket.not_found"),
    REPORT_TARGET_INVALID("report.target_invalid"),;
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
