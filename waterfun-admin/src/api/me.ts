import request from "../utils/axiosRequest"
import type { ISOString, PromiseResBody } from "@waterfun/web-core/src/types/api/response";

export interface AdminAvatarResponse {
    url: string;
    expireAt?: ISOString;
}

export interface AdminUserInfoResponse {
    uid: string;
    username: string;
    nickname: string;
    avatar?: AdminAvatarResponse;
    accountStatus: 'ACTIVE' | 'SUSPENDED' | 'DEACTIVATED';
    createdAt?: ISOString;
    passwordHash: boolean;
    roles: string[];
    permissions: string[];
    isAdmin: boolean;
}

export interface UpdateUserProfileRequest {
    bio?: string;
    gender?: 'MALE' | 'FEMALE' | 'UNKNOWN' | 'OTHER';
    birthday?: string;
    residence?: string;
}


export const getCurrentUserInfo = (): PromiseResBody<AdminUserInfoResponse> => {
    return request.post<AdminUserInfoResponse>('/me/info');
}

export const updateCurrentUserProfile = (data: UpdateUserProfileRequest): PromiseResBody<null> => {
    return request.put<null>('/me/updateProfile', data);
}
