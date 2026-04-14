import request from "../utils/axiosRequest"
import type { LoginRequest, AccessTokenResponse } from "@waterfun/web-core/src/types/api/auth";
import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response";

export const login = (data: LoginRequest): PromiseResBody<AccessTokenResponse> => {
    return request.post('/auth/login-by-password', data);
}

export interface AdminAvatarResponse {
    url: string;
    expireAt?: {
        seconds: number;
        nanos: number;
    };
}

export interface AdminUserInfoResponse {
    uid: string;
    username: string;
    nickname: string;
    avatar?: AdminAvatarResponse;
    accountStatus: 'ACTIVE' | 'SUSPENDED' | 'DEACTIVATED';
    createdAt?: {
        seconds: number;
        nanos: number;
    };
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
    return request.post('/auth/info');
}

export const updateCurrentUserProfile = (data: UpdateUserProfileRequest): PromiseResBody<void> => {
    return request.put('/auth/updateProfile', data);
}

export const logout = (deviceFp: string): PromiseResBody<void> => {
    return request.post('/auth/logout', deviceFp);
}
