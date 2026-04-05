import type { ISOString, OptionResItem, PromiseResBody, ResBody } from "@waterfun/web-core/src/types/api/response";
import type { Page } from "~/types/api";
import request from "~/utils/axiosRequest";

export type AccountStatus = "ACTIVE" | "SUSPENDED" | "DEACTIVATED" | "DELETED";
export type Gender = "MALE" | "FEMALE" | "OTHER" | "UNKNOWN";

export interface UserAdminDto {
  uid: number;
  username: string;
  accountStatus: AccountStatus;
  statusChangedAt: ISOString;
  updatedAt: ISOString;
  createdAt: ISOString;
  nickname: string | null;
  avatarUrl: string | null;
  lastActiveAt: ISOString;
}

export interface UserProfileDto {
  bio: string | null;
  gender: Gender | null;
  birthDate: string | null;
  residence: string | null;
  updateAt: ISOString;
}

export interface UserCounterDto {
  level: number;
  exp: number;
  followerCnt: number;
  followingCnt: number;
  likeCnt: number;
  postCnt: number;
  updatedAt: ISOString;
  visible: boolean;
}

export interface UserMaskedDataDto {
  phoneMasked: string | null;
  emailMasked: string | null;
  phoneVerified: boolean;
  emailVerified: boolean;
}

export interface UserExpirableOption extends OptionResItem<number> {
  expiresAt: ISOString;
}

export interface AssignedRoleRes {
  id: number;
  code: string;
  name: string;
  assignedAt: ISOString;
  expiresAt: ISOString;
}

export interface AssignedPermissionRes {
  id: number;
  code: string;
  name: string;
  assignedAt: ISOString;
  expiresAt: ISOString;
}

export interface UserDetailDto {
  info: UserAdminDto;
  profile: UserProfileDto;
  counter: UserCounterDto;
  maskedData: UserMaskedDataDto;
  roles: UserExpirableOption[];
  permissions: UserExpirableOption[];
}

export interface ListUserParams {
  page?: number;
  size?: number;
  username?: string;
  nickname?: string;
  accountStatus?: AccountStatus;
  createdStart?: ISOString;
  createdEnd?: ISOString;
}

export interface UserInfoUpdateReq {
  username?: string;
  accountStatus?: AccountStatus;
  nickname?: string;
  avatarUrl?: string;
}

export interface UserProfileUpdateReq {
  bio?: string;
  gender?: Gender;
  birthDate?: string;
  residence?: string;
}

export interface UserDatumUpdateReq {
  email?: string;
  phone?: string;
}

export interface UserRoleItemDto {
  roleId: number;
  expiresAt?: ISOString;
}

export interface UserPermItemDto {
  permissionId: number;
  expiresAt?: ISOString;
}

export interface BatchResult {
  requested: number;
  success: number;
  ignored: number;
  failed: number;
  ignoredIds: number[];
  failedIds: number[];
  message: string;
}

export const getUserList = (params: ListUserParams = {}): PromiseResBody<Page<UserAdminDto>> => {
  return request.get<ResBody<Page<UserAdminDto>>>("/users/list", { params });
};

export const getUserDetail = (uid: number): PromiseResBody<UserDetailDto> => {
  return request.get<ResBody<UserDetailDto>>(`/users/${uid}`);
};

export const deleteUser = (uid: number): PromiseResBody<null> => {
  return request.delete<ResBody<null>>(`/users/${uid}`);
};

export const updateUserInfo = (uid: number, data: UserInfoUpdateReq): PromiseResBody<null> => {
  return request.put<ResBody<null>>(`/users/${uid}/info`, data);
};

export const updateUserProfile = (uid: number, data: UserProfileUpdateReq): PromiseResBody<null> => {
  return request.put<ResBody<null>>(`/users/${uid}/profile`, data);
};

export const updateUserDatum = (uid: number, data: UserDatumUpdateReq): PromiseResBody<null> => {
  return request.put<ResBody<null>>(`/users/${uid}/datum`, data);
};

export const listUserRoles = (
  uid: number,
  page: number = 0,
  size: number = 10,
  roleId?: number,
  name?: string,
  code?: string
): PromiseResBody<Page<AssignedRoleRes>> => {
  return request.get<ResBody<Page<AssignedRoleRes>>>(`/users/${uid}/roles`, {
    params: { page, size, roleId, name, code },
  });
};

export const listUserPermissions = (
  uid: number,
  page: number = 0,
  size: number = 10,
  permId?: number,
  name?: string,
  code?: string
): PromiseResBody<Page<AssignedPermissionRes>> => {
  return request.get<ResBody<Page<AssignedPermissionRes>>>(`/users/${uid}/permissions`, {
    params: { page, size, permId, name, code },
  });
};

export const assignUserRoles = (uid: number, userRoles: UserRoleItemDto[]): PromiseResBody<null> => {
  return request.post<ResBody<null>>(`/users/${uid}/roles`, { userRoles });
};

export const replaceUserRoles = (uid: number, userRoleItemDtos: UserRoleItemDto[]): PromiseResBody<null> => {
  return request.put<ResBody<null>>(`/users/${uid}/roles`, { userRoleItemDtos });
};

export const removeUserRoles = (uid: number, roleIds: number[]): PromiseResBody<BatchResult> => {
  return request.delete<ResBody<BatchResult>>(`/users/${uid}/roles`, { data: { roleIds } });
};

export const assignUserPermissions = (uid: number, userPermissions: UserPermItemDto[]): PromiseResBody<null> => {
  return request.post<ResBody<null>>(`/users/${uid}/permissions`, { userPermissions });
};

export const removeUserPermissions = (uid: number, permissionIds: number[]): PromiseResBody<BatchResult> => {
  return request.delete<ResBody<BatchResult>>(`/users/${uid}/permissions`, { data: { permissionIds } });
};

export const getUserOptions = (): PromiseResBody<OptionResItem<number>[]> => {
  return request.get<ResBody<OptionResItem<number>[]>>("/users/options");
};
