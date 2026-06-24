import request from "../utils/axiosRequest";
import type { 
    PromiseResBody,
    CloudResourceUrlResp,
 } from "@waterfun/web-core/src/types/api/response.d.ts";
import { getUploadPolicy, uploadCallback, uploadFileToCos as genericUploadFileToCos, type PresignedResp, type UploadCallbackResp } from "./uploadApi";
import type { UserBrief } from "~/api/postApi";


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

export interface UserProfileDto {
    avatar?: CloudResourceUrlResp;
    nickname?: string;
    bio: string;
    gender: string;
    birthday: string; // ISO 8601 格式日期字符串
    residence: string;
}

/** 符合 OpenAPI UpdateUserProfileRequest 规范 */
export interface UpdateProfileRequest {
    bio: string;
    gender: string;
    birthday: string;
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

export const updateUserProfile = (data: Partial<UpdateProfileRequest>): PromiseResBody<null> => {
    return request.put(`/user/updateProfile`, data)
}

/** 更新昵称（如果后端有独立端点） */
export const updateUserNickname = (nickname: string): PromiseResBody<null> => {
    return request.put(`/user/userInfo`, { nickname })
}

export const getAvatarUploadPolicy = (suffix: string): PromiseResBody<PresignedResp[]> => {
    const ext = suffix.startsWith('.') ? suffix.slice(1) : suffix;
    return getUploadPolicy({
        bizType: 'AVATAR',
        exts: [ext.toUpperCase()]
    });
}

// 导出通用的上传API，防止如果原来的业务组件里正在用它产生冲突
export const uploadFileToCos = genericUploadFileToCos;

export const callbackAvatarUpload = (data: { token: string }): PromiseResBody<UploadCallbackResp> => {
    return uploadCallback(data);
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

export const getPermissions = (): PromiseResBody<string[]> => {
    return request.get(`/user/permissions`);
}

export const followUser = (uid: string): PromiseResBody<void> => {
    return request.post(`/user/${uid}/follow`);
}

export interface PageUserBrief {
    content: UserBrief[]
    totalElements: number
    totalPages: number
    size: number
    number: number
    first: boolean
    last: boolean
    empty: boolean
}

export const getFollowings = (uid: string, page: number = 1, size: number = 20): PromiseResBody<PageUserBrief> => {
    return request.get(`/user/${uid}/followings`, { params: { page, size } });
}

export const getFollowers = (uid: string, page: number = 1, size: number = 20): PromiseResBody<PageUserBrief> => {
    return request.get(`/user/${uid}/followers`, { params: { page, size } });
}