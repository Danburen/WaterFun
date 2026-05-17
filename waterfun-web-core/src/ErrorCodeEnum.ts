// Basic http status
import type { ToCamelCase } from "./dataMapper"

export enum HttpStatus {
    OK = 200,
    BAD_REQUEST = 400,
    UNAUTHORIZED = 401,
    FORBIDDEN = 403,
    NOT_FOUND = 404,
    INTERNAL_SERVER_ERROR = 500
}

// Service error code
export enum ErrorCode {
    // general error
    UNKNOWN_ERROR = 50000,

    // user info related error
    USERNAME_EMPTY_OR_INVALID = 40001,
    PASSWORD_EMPTY_OR_INVALID = 40002,
    USERNAME_OR_PASSWORD_INCORRECT = 40003,
    CAPTCHA_EXPIRED = 40004,
    CAPTCHA_INCORRECT = 40005,
    VERIFY_CODE_EXPIRED = 40006,
    VERIFY_CODE_INCORRECT = 40007,
    SMS_CODE_EXPIRED = 40008,
    SMS_CODE_INCORRECT = 40009,
    EMAIL_CODE_EXPIRED = 40010,
    EMAIL_CODE_INCORRECT = 40011,
    CAPTCHA_EMPTY = 40012,
    SMS_CODE_EMPTY = 40013,
    EMAIL_CODE_EMPTY = 40014,
    PHONE_NUMBER_EMPTY_OR_INVALID = 40015,
    EMAIL_ADDRESS_EMPTY_OR_INVALID = 40016,
    USER_ALREADY_EXISTS = 40017,
    USER_NOT_FOUND = 40018,

    // user role error
    ROLE_NOT_FOUND = 40019,
    ROLE_ALREADY_EXISTS = 40020,
    PERMISSION_NOT_FOUND = 40021,
    PERMISSION_ALREADY_EXISTS = 40022,
    REDUNDANT_OPERATION = 40023,
    INVALID_PATH = 40024,
    REQUEST_NOT_IN_WHITELIST = 40025,
    INVALID_CONTENT_TYPE = 40026,

    // auth error
    ACCESS_TOKEN_EXPIRED = 40101,
    ACCESS_TOKEN_INVALID = 40102,
    ACCESS_TOKEN_MISSING = 40103,
    REFRESH_TOKEN_EXPIRED = 40104,
    REFRESH_TOKEN_INVALID = 40105,
    REFRESH_TOKEN_MISSING = 40106
}

export type AutoErrorCodeMap<T extends Record<string, number>> = {
    [K in keyof T as K extends string ? ToCamelCase<K> : never]: K;
};

const ErrorCodeMap: Record<string, number> = Object.entries(ErrorCode)
    .filter(([key]) => isNaN(Number(key))) // 过滤掉反向映射的数字键
    .reduce((acc, [key, value]) => ({ ...acc, [key]: value }), {});

export type ErrorMessageKeyAuto = keyof AutoErrorCodeMap<typeof ErrorCodeMap>

export const AUTO_ERROR_CODE_MESSAGE_KEY_MAP = createAutoErrorMap(ErrorCodeMap);

export function createAutoErrorMap<T extends Record<string, number>>(enumObj: T): Record<number, ToCamelCase<keyof T & string>> {
    const result: Record<number, string> = {};

    Object.keys(enumObj).forEach(key => {
        if (isNaN(Number(key))) {
            const value = enumObj[key as keyof typeof enumObj];
            const camelKey = key
                .toLowerCase()
                .replace(/_([a-z])/g, (_, letter) => letter.toUpperCase());
            //@ts-ignore
            result[value] = camelKey as ToCamelCase<keyof T & string>;
        }
    });

    return result as Record<number, ToCamelCase<keyof T & string>>;
}