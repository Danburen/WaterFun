import type { ISOString, OptionResItem, PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import type { Page } from "~/types/api";
import request from "~/utils/axiosRequest";

export type AccountStatus = "ACTIVE" | "SUSPENDED" | "DEACTIVATED" | "DELETED";
export type Gender = "MALE" | "FEMALE" | "OTHER" | "UNKNOWN";
export type Uid = string;

export interface UserAdminDto {
  uid: Uid;
  username: string;
  userType: number;
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

export interface RemoveUsersReq {
  userUids: Uid[];
}

export interface CreateNewUserReq {
  phone?: string;
  username: string;
  password?: string;
  userType: number;
}

export const getUserList = (params: ListUserParams = {}): PromiseResBody<Page<UserAdminDto>> => {
  return request.get<Page<UserAdminDto>>("/users/list", { params });
};

export const getUserDetail = (uid: Uid | number): PromiseResBody<UserDetailDto> => {
  return request.get<UserDetailDto>(`/users/${String(uid)}`);
};

export const createUser = (data: CreateNewUserReq): PromiseResBody<null> => {
  return request.post<null>("/users", data);
};

export const deleteUser = (uid: Uid | number): PromiseResBody<null> => {
  return request.delete<null>(`/users/${String(uid)}`);
};

export const deleteUsers = (userUids: Array<Uid | number>): PromiseResBody<BatchResult> => {
  const data: RemoveUsersReq = { userUids: userUids.map((uid) => String(uid)) };
  return request.delete<BatchResult>("/users", { data });
};

export const updateUserInfo = (uid: Uid | number, data: UserInfoUpdateReq): PromiseResBody<null> => {
  return request.put<null>(`/users/${String(uid)}/info`, data);
};

export const updateUserProfile = (uid: Uid | number, data: UserProfileUpdateReq): PromiseResBody<null> => {
  return request.put<null>(`/users/${String(uid)}/profile`, data);
};

export const updateUserDatum = (uid: Uid | number, data: UserDatumUpdateReq): PromiseResBody<null> => {
  return request.put<null>(`/users/${String(uid)}/datum`, data);
};

export const listUserRoles = (
  uid: Uid | number,
  page: number = 0,
  size: number = 10,
  roleId?: number,
  name?: string,
  code?: string
): PromiseResBody<Page<AssignedRoleRes>> => {
  return request.get<Page<AssignedRoleRes>>(`/users/${String(uid)}/roles`, {
    params: { page, size, roleId, name, code },
  });
};

export const listUserPermissions = (
  uid: Uid | number,
  page: number = 0,
  size: number = 10,
  permId?: number,
  name?: string,
  code?: string
): PromiseResBody<Page<AssignedPermissionRes>> => {
  return request.get<Page<AssignedPermissionRes>>(`/users/${String(uid)}/permissions`, {
    params: { page, size, permId, name, code },
  });
};

export const assignUserRoles = (uid: Uid | number, userRoles: UserRoleItemDto[]): PromiseResBody<null> => {
  return request.post<null>(`/users/${String(uid)}/roles`, { userRoles });
};

export const replaceUserRoles = (uid: Uid | number, userRoleItemDtos: UserRoleItemDto[]): PromiseResBody<null> => {
  return request.put<null>(`/users/${String(uid)}/roles`, { userRoleItemDtos });
};

export const removeUserRoles = (uid: Uid | number, roleIds: number[]): PromiseResBody<BatchResult> => {
  return request.delete<BatchResult>(`/users/${String(uid)}/roles`, { data: { roleIds } });
};

export const assignUserPermissions = (uid: Uid | number, userPermissions: UserPermItemDto[]): PromiseResBody<null> => {
  return request.post<null>(`/users/${String(uid)}/permissions`, { userPermissions });
};

export const removeUserPermissions = (uid: Uid | number, permissionIds: number[]): PromiseResBody<BatchResult> => {
  return request.delete<BatchResult>(`/users/${String(uid)}/permissions`, { data: { permissionIds } });
};

export const getUserOptions = (): PromiseResBody<OptionResItem<Uid>[]> => {
  return request.get<OptionResItem<Uid>[]>("/users/options");
};
