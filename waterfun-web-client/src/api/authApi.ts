import request from "../utils/axiosRequest"

import type { AccessTokenResponse, DeviceInfo } from "@waterfun/web-core/src/types/api/auth";
import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import { getDeviceInfo } from "@waterfun/web-core/src/fingerprint";

interface BaseLoginRequest {
    deviceFp: string
    deviceInfo?: DeviceInfo
}

interface BasicRegisterRequest {
    phone: string
    smsCode: string
    verify: VerifyCodeDto
}

interface FullRegisterRequest extends BasicRegisterRequest {
    email?: string
    password?: string
}

interface PasswordLoginRequest extends BaseLoginRequest {
    username: string
    password: string
    captcha: string
}

interface CodeLoginRequest extends VerifyCodeDto {
    scene: 'login'
}

export type LoginRequest = PasswordLoginRequest | CodeLoginRequest
export type RegisterRequest = BasicRegisterRequest | FullRegisterRequest

export type VerifyScene = 'login' | 'register' | 'forgot_password' | SecurityVerifyScene

export type SecurityVerifyScene = 'reset_password' | 'set_password' |
    'change_email' | 'change_phone' | 'verify' | 'activate' | 'bind_email' | 'unbind'

export type SendCodeType = {
    target: string
    channel: 'sms' | 'email'
    scene: VerifyScene
    deviceFp?: string
    deviceInfo?: DeviceInfo
    captcha?: string
}

export interface VerifyCodeDto {
    target: string
    code: string
    channel: 'sms' | 'email'
    scene: VerifyScene
    deviceFp?: string
    deviceInfo?: DeviceInfo
}

async function enrichWithDeviceInfo<T extends { deviceFp: string; deviceInfo?: DeviceInfo }>(body: T): Promise<T> {
    if (!body.deviceInfo) {
        body.deviceInfo = await getDeviceInfo()
    }
    return body
}

export const login = async (loginRequest: LoginRequest, type: string): PromiseResBody<AccessTokenResponse> => {
    const body = await enrichWithDeviceInfo(loginRequest as any)
    if(type == 'password'){
        return request.post("/auth/login-by-password", body)
    }else{
        return request.post("/auth/login-by-code", body)
    }
}

export const register = async (registerRequest: RegisterRequest): PromiseResBody<AccessTokenResponse> => {
    const body = registerRequest as any
    if (body.verify && !body.verify.deviceInfo) {
        const di = await getDeviceInfo()
        body.verify.deviceInfo = di
        if (!body.verify.deviceFp) body.verify.deviceFp = di.deviceFp
    }
    return request.post("/auth/register", body)
}

export const getCaptcha = (): Promise<ArrayBuffer> => {
    return request.get<never, ArrayBuffer>('/auth/captcha', {
        responseType: 'arraybuffer',
    });
}

export const sendCode = async (sendCodeData: SendCodeType): PromiseResBody<void> => {
    const body = await enrichWithDeviceInfo(sendCodeData as any)
    return request.post('/auth/send-code', body)
}

export const logout = async (deviceFp: string): PromiseResBody<void> => {
    return request.post('/user/security/logout', { deviceFp })
}


export interface ForgotPasswordRequest {
    target: string
    code: string
    newPwd: string
    confirmPwd: string
    deviceInfo?: DeviceInfo
}

export interface ReAuthKeyVo {
    reAuthKey: string;
}

export interface ReAuthTokenVo {
    reAuthToken: string;
}

// -- 忘记密码 re-auth 流程 --

export const forgotPasswordReAuth = (identifier: string, captcha: string): PromiseResBody<ReAuthKeyVo> => {
    return request.post('/auth/forgot-password/re-auth', { identifier, captcha });
}

export const forgotPasswordVerifyReAuth = (reAuthKey: string, code: string): PromiseResBody<ReAuthTokenVo> => {
    return request.post('/auth/forgot-password/re-auth/verify', { reAuthKey, code });
}

export const forgotPasswordReset = async (reAuthToken: string, newPwd: string, confirmPwd: string): PromiseResBody<void> => {
    return request.post('/auth/forgot-password/reset', { reAuthToken, newPwd, confirmPwd })
}

export const refreshAccessToken = (deviceFp: string): PromiseResBody<AccessTokenResponse> => {
    return request.post('/auth/refresh', null, { params: { deviceFp } });
}
