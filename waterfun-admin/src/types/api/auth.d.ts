interface BaseLoginRequest {
    username: string,
    deviceFp: string,
}

interface BasicRegisterRequest {
    phone: string,
    smsCode: string,
    deviceFp: string,
}

interface FullRegisterRequest extends BasicRegisterRequest {
    email?: string,
    password?: string,
}

interface PasswordLoginRequest extends BaseLoginRequest {
    username: string;
    password: string;
    captcha: string;
    smsCode?: never;
    emailCode?: never;
}

interface SmsLoginRequest extends BaseLoginRequest {
    phoneNumber: string;
    smsCode: string;
    password?: string;
    captcha?: never;
    emailCode?: never;
}

interface EmailLoginRequest extends BaseLoginRequest {
    email: string;
    emailCode: string;
    password?: never;
    captcha?: never;
    smsCode?: never;
}

export type LoginRequest = PasswordLoginRequest | EmailLoginRequest | SmsLoginRequest;
export type RegisterRequest = BasicRegisterRequest | FullRegisterRequest;
export type SendCodeType = {
    phone?: string;
    email?: string;
    purpose: 'login' | 'register' | 'resetPassword'
}