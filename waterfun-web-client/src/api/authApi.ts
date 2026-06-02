import request from "../utils/axiosRequest"

import type { AccessTokenResponse } from "@waterfun/web-core/src/types/api/auth";
import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response";

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

interface CodeLoginRequest extends VerifyCodeDto {
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

export const login = (loginRequest: LoginRequest, type: string): PromiseResBody<AccessTokenResponse> => {
    if(type == 'password'){
        return request.post("/auth/login-by-password", loginRequest);
    }else{
        return request.post("/auth/login-by-code", loginRequest);
    }
}

export const register = (registerRequest: RegisterRequest): PromiseResBody<AccessTokenResponse> => {
    return request.post("/auth/register", registerRequest);
}

export const getCaptcha = (): Promise<ArrayBuffer> => {
    return request.get<never, ArrayBuffer>('/auth/captcha', {
        responseType: 'arraybuffer',
        meta: { needCSRF: false }
    });
}

export const sendCode = (sendCodeData: SendCodeType): PromiseResBody<void> => {
    return request.post('/auth/send-code', sendCodeData);
}

export const sendAuthenticationCode = (sendCodeData: SecuritySendCodeType): PromiseResBody<void> => {
    return request.post('/auth/security/send-verify-code', sendCodeData);
}

export const logout = (deviceFp: string): PromiseResBody<void> => {
    return request.post('/auth/logout', deviceFp);
}

export const getCsrfToken = (): PromiseResBody<void> => {
    return request.get('/auth/csrf-token');
}

export const refreshAccessToken = (deviceFp?: string): PromiseResBody<AccessTokenResponse> => {
    return request.post('/auth/refresh', null, { params: { deviceFp } });
}
