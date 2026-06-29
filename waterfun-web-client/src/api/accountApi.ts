import request from "../utils/axiosRequest"
import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import type { SecurityVerifyCodeDto, SendCodeType } from "~/api/authApi";

export interface AccountInfo {
    phoneMasked: string;
    emailMasked: string;
    phoneVerified: boolean;
    emailVerified: boolean;
}

export interface ResetPasswordRequest {
    oldPwd: string;
    newPwd: string;
    confirmPwd: string;
    verify: SecurityVerifyCodeDto;
}

export interface SetPasswordRequest {
    newPwd: string;
    confirmPwd: string;
    verify: SecurityVerifyCodeDto;
}

export interface BindEmailRequest {
    email: string;
    verify: SecurityVerifyCodeDto;
}

export interface ActivateEmailRequest {
    email: string;
    verify: SecurityVerifyCodeDto;
}

export interface ChangeEmailRequest {
    email: string;
    verify: SecurityVerifyCodeDto;
}

export interface UnbindEmailRequest {
    email: string;
    verify: SecurityVerifyCodeDto;
}

export interface ActivatePhoneRequest {
    phone: string;
    verify: SecurityVerifyCodeDto;
}

export interface ChangePhoneRequest {
    phone: string;
    verify: SecurityVerifyCodeDto;
}

export const getAccountInfo = (): PromiseResBody<AccountInfo> => {
    return request.get('/auth/account');
}

export const resetPassword = (resetPasswordData: ResetPasswordRequest): PromiseResBody<void> => {
    return request.post('/auth/account/password/reset', resetPasswordData);
}

export const setPassword = (setPasswordData: SetPasswordRequest): PromiseResBody<void> => {
    return request.post('/auth/account/password/set', setPasswordData);
}

export const sendVerifyCode = (sendCodeData: SendCodeType): PromiseResBody<void> => {
    return request.post('/auth/account/send-verify-code', sendCodeData);
}

export const bindEmail = (bindEmailData: BindEmailRequest): PromiseResBody<void> => {
    return request.post('/auth/account/email/bind', bindEmailData);
}

export const activateEmail = (activateEmailData: ActivateEmailRequest): PromiseResBody<void> => {
    return request.post('/auth/account/email/activate', activateEmailData);
}

export const changeEmail = (changeEmailData: ChangeEmailRequest): PromiseResBody<void> => {
    return request.post('/auth/account/email/change', changeEmailData);
}
    
export const activatePhone = (activatePhoneData: ActivatePhoneRequest): PromiseResBody<void> => {
    return request.post('/auth/account/phone/activate', activatePhoneData);
}
    
export const changePhone = (changePhoneData: ChangePhoneRequest): PromiseResBody<void> => {
    return request.post('/auth/account/phone/change', changePhoneData);
}

export const unbindEmail = (unbindEmailData: UnbindEmailRequest): PromiseResBody<void> => {
    return request.post('/auth/account/email/unbind', unbindEmailData);
}