import request from "../utils/axiosRequest";
import type { 
    PromiseResBody,
    CloudResourceUrlResp,
    UploadPolicyResponse,
    UploadCallbackRequest
 } from "@waterfun/web-core/src/types/api/response.d.ts";


export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

export interface UserInfoResponse {
    uid: string;
    username: string;
    nickname: string;
    avatar: CloudResourceUrlResp;
    accountStatus: string;
    createdAt: string;
    passwordHash: boolean;
}

export interface UserProfileDto{
    avatar?: CloudResourceUrlResp;
    nickname?: string;
    bio: string;
    gender: string;
    birthday: string; // ISO 8601 格式日期字符串
    residence: string;
}

export interface UserNotificationSettings {
    messageNotifications: boolean;
    commentNotifications: boolean;
    likeNotifications: boolean;
    followNotifications: boolean;
    eventNotifications: boolean;
    emailNotifications: boolean;
}

export interface UserPrivacySettings {
    profileVisibility: string;
    workVisibility: string;
    commentPermission: string;
    messagePermission: string;
    allowFollow: boolean;
    showActiveStatus: boolean;
}

export interface UserSettingsDto {
    notifications: UserNotificationSettings;
    privacy: UserPrivacySettings;
}

export const getUserInfo = (): PromiseResBody<UserInfoResponse> => {
    return request.get(`/user/userInfo`, {})
}

export const getUserProfile = (): PromiseResBody<UserProfileDto> => {
    return request.get(`/user/profile`, {})
}

export const updateUserProfile = (data: Partial<UserProfileDto>): PromiseResBody<null> => {
    return request.put(`/user/updateProfile`, data)
}

export const getAvatarUploadPolicy = (suffix: string): PromiseResBody<UploadPolicyResponse> => {
    return request.get(`/user/avatar/upload?suffix=${encodeURIComponent(suffix)}`)
}

export const uploadFileToCos = (url: string, method: HttpMethod | string, file: File): Promise<Response> => {
    const uppercaseMethod = method.toUpperCase();
    const fetchOptions: RequestInit = {
        method: uppercaseMethod,
        body: file,
        headers: {}
    };
    return fetch(url, fetchOptions)
}

export const callbackAvatarUpload = (data: UploadCallbackRequest): PromiseResBody<null> => {
    return request.post(`/user/avatar/upload/callback`, data)
}

export const getAvatar = (): PromiseResBody<CloudResourceUrlResp> => {
    return request.get(`/user/avatar`);
}

export const getUserSettings = (): PromiseResBody<UserSettingsDto> => {
    return request.get(`/user/settings`, {});
}

export const updateUserSettings = (data: UserSettingsDto): PromiseResBody<null> => {
    return request.put(`/user/settings`, data);
}