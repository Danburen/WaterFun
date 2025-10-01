interface BaseLoginRequest {
    username: string,
    deviceFp: string,
    loginType: LoginType,
}

interface BasicRegisterRequest {
    phone: string,
    username: string,
    smsCode: string,
    deviceFp: string,
}

interface FullRegisterRequest extends BasicRegisterRequest {
    email?: string,
    password?: string,
}

interface PasswordLoginRequest extends BaseLoginRequest {
    loginType: 'password';
    password: string;
    captcha: string;
    smsCode?: never;
    emailCode?: never;
}

interface SmsLoginRequest extends BaseLoginRequest {
    loginType: 'sms'
    smsCode: string;
    password?: string;
    captcha?: never;
    emailCode?: never;
}

interface EmailLoginRequest extends BaseLoginRequest {
    loginType: 'email';
    emailCode: string;
    password?: never;
    captcha?: never;
    smsCode?: never;
}

export type LoginType = 'password' | 'email' | 'sms'
export type LoginRequest = PasswordLoginRequest | EmailLoginRequest | SmsLoginRequest;
export type RegisterRequest = BasicRegisterRequest | FullRegisterRequest;
export type SendCodeType = {
    phoneNumber?: string;
    email?: string;
    purpose: 'login' | 'register' | 'resetPassword'
}