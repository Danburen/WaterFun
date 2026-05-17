interface BaseLoginRequest {
    deviceFp: string,
}

interface BasicRegisterRequest {
    phone: string,
    smsCode: string,
    verify: VerifyCodeDto
}

interface FullRegisterRequest extends BasicRegisterRequest {
    email?: string,
    password?: string,
}

interface PasswordLoginRequest extends BaseLoginRequest {
    username: string;
    password: string;
    captcha: string;
}

interface CodeLoginRequest extends BaseLoginRequest, VerifyCodeDto {
    scene: 'login'
}


export type LoginRequest = PasswordLoginRequest | CodeLoginRequest;
export type RegisterRequest = BasicRegisterRequest | FullRegisterRequest;

export type VerifyScene = 'login' | 'register' | SecurityVerifyScene;

export type SecurityVerifyScene = 'reset_password' | 'set_password' |
    'change_email' | 'change_phone' | 'verify' | 'activate' | 'bind_email' | 'unbind';

export type SendCodeType = {
    target: string,
    channel: 'sms' | 'email',
    scene: VerifyScene,
    deviceFp?: string,
}

export type SecuritySendCodeType = {
    channel: 'sms' | 'email',
    scene: VerifyScene,
    deviceFp?: string,
}

export interface VerifyCodeDto {
    target: string,
    code: string,
    channel: 'sms' | 'email',
    scene: VerifyScene,
    deviceFp?: string,
}

export interface SecurityVerifyCodeDto {
    code: string,
    channel: 'sms' | 'email',
    scene: VerifyScene,
    deviceFp?: string,
}

export interface AccessTokenResponse {
    accessToken: string,
    exp: number,
}
