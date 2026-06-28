import request from "../utils/axiosRequest"
import type { ISOString, PromiseResBody } from "@waterfun/web-core/src/types/api/response";

export interface AdminAvatarResponse {
    url?: string;
    expireAt?: ISOString;
}

export interface AdminUserInfoResponse {
    uid: string;
    username: string;
    nickname: string;
    avatar?: AdminAvatarResponse | null;
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

export interface UserProfile {
    bio: string | null;
    gender: 'MALE' | 'FEMALE' | 'UNKNOWN' | 'OTHER' | null;
    birthday: string | null;
    residence: string | null;
}

export interface UserRole {
    id: number;
    role: { id: number; code: string; name: string };
    expiresAt: ISOString | null;
}

export interface UserPermission {
    id: number;
    permission: { id: number; code: string; name: string };
    expiresAt: ISOString | null;
}

export const getCurrentUserInfo = (): PromiseResBody<AdminUserInfoResponse> => {
    return request.get<AdminUserInfoResponse>('/me/info');
}

export const getCurrentUserProfile = (): PromiseResBody<UserProfile> => {
    return request.get<UserProfile>('/me/profile');
}

export const getCurrentUserRoles = (): PromiseResBody<UserRole[]> => {
    return request.get<UserRole[]>('/me/roles');
}

export const getCurrentUserPermissions = (): PromiseResBody<UserPermission[]> => {
    return request.get<UserPermission[]>('/me/permissions');
}

export const updateCurrentUserProfile = (data: UpdateUserProfileRequest): PromiseResBody<null> => {
    return request.put<null>('/me/updateProfile', data);
}
