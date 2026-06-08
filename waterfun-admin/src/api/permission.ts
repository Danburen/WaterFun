import type {
  ISOString,
  OptionResItem,
  PromiseResBody,
} from "@waterfun/web-core/src/types/api/response";
import type { Page } from "~/types/api";
import request from "~/utils/axiosRequest";

export type PermissionType = "MENU" | "BUTTON" | "API" | "DATA" | "OTHER";

export interface PermissionResp {
  id: number;
  code: string;
  name: string;
  orderWeight: number | null;
  description: string | null;
  type: PermissionType;
  resource: string;
  isSystem: boolean;
  parentId: number | null;
  createdAt: ISOString;
  updatedAt: ISOString;
}

export interface ListPermissionParams {
  page?: number;
  size?: number;
  name?: string;
  code?: string;
  type?: PermissionType;
  resource?: string;
  parentId?: number;
}

export interface CreatePermRequest {
  code?: string;
  name: string;
  description?: string;
  type?: PermissionType;
  resource: string;
  parentId?: number;
  orderWeight?: number;
  isSystem?: boolean;
}

export interface UpdatePermRequest {
  id?: number;
  code?: string;
  name: string;
  description?: string;
  type?: PermissionType;
  resource: string;
  parentId?: number;
  orderWeight?: number;
  isSystem?: boolean;
}

export type AccountStatus = "ACTIVE" | "SUSPENDED" | "DEACTIVATED" | "DELETED";

export interface UserInfoARes {
  uid: string;
  username: string;
  accountStatus: AccountStatus;
  statusChangedAt: ISOString;
  updatedAt: ISOString;
  createdAt: ISOString;
  nickname: string;
  avatarUrl: string;
  lastActiveAt: ISOString;
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

export interface ExpirableOption<TId extends number | string = number> extends OptionResItem<TId> {
  expiresAt: ISOString;
}

export interface AssignedUserRes {
  userUid: string;
  username: string;
  nickname: string;
  assignedAt: ISOString;
  expiresAt: ISOString;
}

export interface AssignPermToUsersReq {
  ids?: string[];
  expiresAt?: ISOString;
}

export interface RemovePermUsersReq {
  userUids?: string[];
}

export interface DeletePermsRequest {
  permIds: number[];
}

export const listPermissions = (
  params: ListPermissionParams = {}
): PromiseResBody<Page<PermissionResp>> => {
  return request.get<Page<PermissionResp>>("/permission/list", { params });
};

export const getPermission = (id: number): PromiseResBody<PermissionResp> => {
  return request.get<PermissionResp>(`/permission/${id}`);
};

export const addPermission = (data: CreatePermRequest): PromiseResBody<null> => {
  return request.post<null>("/permission", data);
};

export const updatePermission = (id: number, data: UpdatePermRequest): PromiseResBody<null> => {
  return request.put<null>(`/permission/${id}`, data);
};

export const deletePermission = (id: number): PromiseResBody<null> => {
  return request.delete<null>(`/permission/${id}`);
};

export const deletePerms = (permIds: number[]): PromiseResBody<BatchResult> => {
  const data: DeletePermsRequest = { permIds };
  return request.delete<BatchResult>("/permission", { data });
};

export const getPermUsers = (
  id: number,
  page: number = 0,
  size: number = 10,
  userUid?: string,
  username?: string,
  nickname?: string
): PromiseResBody<Page<AssignedUserRes>> => {
  return request.get<Page<AssignedUserRes>>(`/permission/${id}/users`, {
    params: { page, size, userUid, username, nickname },
  });
};

export const assignPermToUsers = (
  id: number,
  data: AssignPermToUsersReq
): PromiseResBody<BatchResult> => {
  return request.post<BatchResult>(`/permission/${id}/users`, data);
};

export const putPermUsers = (
  id: number,
  data: AssignPermToUsersReq
): PromiseResBody<BatchResult> => {
  return request.put<BatchResult>(`/permission/${id}/users`, data);
};

export const deletePermUsers = (
  id: number,
  data: RemovePermUsersReq
): PromiseResBody<BatchResult> => {
  return request.delete<BatchResult>(`/permission/${id}/users`, { data });
};

export const getPermOptions = (): PromiseResBody<OptionResItem<number>[]> => {
  return request.get<OptionResItem<number>[]>("/permission/options");
};
