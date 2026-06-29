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

export type VerifyScene = 'login' | 'register' | SecurityVerifyScene

export type SecurityVerifyScene = 'reset_password' | 'set_password' |
    'change_email' | 'change_phone' | 'verify' | 'activate' | 'bind_email' | 'unbind'

export type SendCodeType = {
    target: string
    channel: 'sms' | 'email'
    scene: VerifyScene
    deviceFp?: string
    deviceInfo?: DeviceInfo
}

export type SecuritySendCodeType = {
    channel: 'sms' | 'email'
    scene: VerifyScene
    deviceFp?: string
    deviceInfo?: DeviceInfo
}

export interface VerifyCodeDto {
    target: string
    code: string
    channel: 'sms' | 'email'
    scene: VerifyScene
    deviceFp?: string
    deviceInfo?: DeviceInfo
}

export interface SecurityVerifyCodeDto {
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
        meta: { needCSRF: false }
    });
}

export const sendCode = async (sendCodeData: SendCodeType): PromiseResBody<void> => {
    const body = await enrichWithDeviceInfo(sendCodeData as any)
    return request.post('/auth/send-code', body)
}

export const sendAuthenticationCode = async (sendCodeData: SecuritySendCodeType): PromiseResBody<void> => {
    const body = await enrichWithDeviceInfo(sendCodeData as any)
    return request.post('/user/security/send-verify-code', body)
}

export const logout = async (deviceFp: string): PromiseResBody<void> => {
    return request.post('/user/security/logout', { deviceFp })
}

export const getCsrfToken = (): PromiseResBody<void> => {
    return request.get('/auth/csrf-token');
}

export const refreshAccessToken = (deviceFp: string): PromiseResBody<AccessTokenResponse> => {
    return request.post('/auth/refresh', null, { params: { deviceFp } });
}
