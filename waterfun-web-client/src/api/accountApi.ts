import request from "../utils/axiosRequest"
import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import type { VerifyScene } from "~/api/authApi";

export interface AccountInfo {
    phoneMasked: string;
    emailMasked: string;
    phoneVerified: boolean;
    emailVerified: boolean;
}

export const getAccountInfo = (): PromiseResBody<AccountInfo> => {
    return request.get('/auth/account');
}

export interface ReAuthInfo {
    maskedPhone: string;
}

export interface ReAuthTokenVo {
    reAuthToken: string;
}

export interface ChangeTargetResp {
    verifyKey: string;
}

// -- re-auth 统一验证 API --

export const getReAuthInfo = (): PromiseResBody<ReAuthInfo> => {
    return request.get('/auth/account/re-auth/info');
}

export const sendReAuthCode = (scene: VerifyScene): PromiseResBody<void> => {
    return request.post('/auth/account/re-auth', { scene });
}

export const verifyReAuthCode = (scene: VerifyScene, code: string): PromiseResBody<ReAuthTokenVo> => {
    return request.post('/auth/account/re-auth/verify', { scene, code });
}

// -- 操作接口（均使用 reAuthToken） --

export const resetPassword = (newPwd: string, confirmPwd: string, reAuthToken: string): PromiseResBody<void> => {
    return request.post('/auth/account/password/reset', { reAuthToken, newPwd, confirmPwd });
}

export const setPassword = (newPwd: string, confirmPwd: string, reAuthToken: string): PromiseResBody<void> => {
    return request.post('/auth/account/password/set', { reAuthToken, newPwd, confirmPwd });
}

export const activateEmail = (email: string, reAuthToken: string): PromiseResBody<void> => {
    return request.post('/auth/account/email/activate', { reAuthToken, email });
}

export const changeEmail = (newEmail: string, reAuthToken: string): PromiseResBody<ChangeTargetResp> => {
    return request.post('/auth/account/email/change', { reAuthToken, newEmail });
}

export const verifyChangeEmail = (verifyKey: string, code: string): PromiseResBody<void> => {
    return request.post('/auth/account/email/change/verify', { verifyKey, code });
}

export const activatePhone = (phone: string, reAuthToken: string): PromiseResBody<void> => {
    return request.post('/auth/account/phone/activate', { reAuthToken, phone });
}

export const changePhone = (newPhone: string, reAuthToken: string): PromiseResBody<ChangeTargetResp> => {
    return request.post('/auth/account/phone/change', { reAuthToken, newPhone });
}

export const verifyChangePhone = (verifyKey: string, code: string): PromiseResBody<void> => {
    return request.post('/auth/account/phone/change/verify', { verifyKey, code });
}

export const unbindEmail = (email: string, reAuthToken: string): PromiseResBody<void> => {
    return request.post('/auth/account/email/unbind', { reAuthToken, email });
}
