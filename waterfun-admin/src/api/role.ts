import type { ISOString, OptionResItem, PromiseResBody, ResBody } from "@waterfun/web-core/src/types/api/response";
import type { Page } from "~/types/api";
import request from "../utils/axiosRequest";

export interface RoleResp {
    id: number;
    code: string;
    name: string;
    orderWeight: number;
    description: string;
    parentId: number | null;
    isSystem: boolean;
    createdAt: ISOString;
    updatedAt: ISOString;
}

export interface CreateRoleRequest {
    name: string;
    description?: string;
    parentId?: number;
    code?: string;
    orderWeight?: number;
    isSystem?: boolean;
}

export interface UpdateRoleRequest {
    name: string;
    code?: string;
    description?: string;
    parentId?: number;
    orderWeight?: number;
    isSystem?: boolean;
}

export interface RolePermItemDTO {
    permissionId: number;
    expiresAt?: ISOString;
}

export interface AssignRolePermReq {
    perms: RolePermItemDTO[];
}

export interface UpdateRolePermReq {
    perms?: RolePermItemDTO[];
}

export interface DeleteRolePermsReq {
    ids?: number[];
}

export interface ListRolePermsParams {
    page?: number;
    size?: number;
    name?: string;
    code?: string;
}

export interface ListRoleUsersParams {
    page?: number;
    size?: number;
    userUid?: number;
    username?: string;
    nickname?: string;
}

export interface ExpirableOption<TId extends number | string = number> extends OptionResItem<TId> {
    expiresAt: ISOString;
}

export interface AssignedPermissionRes {
    id: number;
    code: string;
    name: string;
    assignedAt: ISOString;
    expiresAt: ISOString;
}

export interface AssignedUserRes {
    userUid: number;
    username: string;
    nickname: string;
    assignedAt: ISOString;
    expiresAt: ISOString;
}

export type PermissionType = "MENU" | "BUTTON" | "API" | "DATA" | "OTHER";

export interface PermissionResp {
    id: number;
    code: string;
    name: string;
    description: string;
    type: PermissionType;
    resource: string;
    parentId: number | null;
    createdAt: ISOString;
}

export type AccountStatus = "ACTIVE" | "SUSPENDED" | "DEACTIVATED" | "DELETED";

export interface UserInfoARes {
    uid: number;
    username: string;
    userType: number;
    accountStatus: AccountStatus;
    statusChangedAt: ISOString;
    updatedAt: ISOString;
    createdAt: ISOString;
    nickname: string;
    avatarUrl: string;
    lastActiveAt: ISOString;
}

export interface AssignUserToRoleReq {
    userUids?: number[];
    expiresAt?: ISOString;
}

export interface RemoveRoleUsersReq {
    userIds?: number[];
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

export interface DeleteRolesRequest {
    roleIds: number[];
}

export const listRoles = (
    page: number = 0,
    size: number = 10,
    name?: string,
    code?: string,
    parentId?: number
): Promise<ResBody<Page<RoleResp>>> => {
    return request.get<ResBody<Page<RoleResp>>>("/role/list", {
        params: { page, size, name, code, parentId }
    });
};

export const getRole = (id: number): Promise<ResBody<RoleResp>> => {
    return request.get<ResBody<RoleResp>>(`/role/${id}`);
};

export const addRole = (data: CreateRoleRequest): Promise<ResBody<null>> => {
    return request.post<ResBody<null>>("/role", data);
};

export const updateRole = (id: number, data: UpdateRoleRequest): Promise<ResBody<null>> => {
    return request.put<ResBody<null>>(`/role/${id}`, data);
};

export const deleteRole = (id: number): Promise<ResBody<null>> => {
    return request.delete<ResBody<null>>(`/role/${id}`);
};

export const deleteRoles = (roleIds: number[]): Promise<ResBody<BatchResult>> => {
    const data: DeleteRolesRequest = { roleIds };
    return request.delete<ResBody<BatchResult>>("/role", { data });
};

export const assignPermissions = (id: number, data: AssignRolePermReq): Promise<ResBody<BatchResult | null>> => {
    return request.post<ResBody<BatchResult | null>>(`/role/${id}/permissions`, data);
};

export const updatePermissions = (id: number, data: UpdateRolePermReq): Promise<ResBody<BatchResult | null>> => {
    return request.put<ResBody<BatchResult | null>>(`/role/${id}/permissions`, data);
};

export const listRolePerms = (
    id: number,
    params: ListRolePermsParams = {}
): PromiseResBody<Page<AssignedPermissionRes>> => {
    return request.get<ResBody<Page<AssignedPermissionRes>>>(`/role/${id}/permissions`, { params });
};

export const deleteRolePerms = (id: number, data: DeleteRolePermsReq): Promise<ResBody<BatchResult | null>> => {
    return request.delete<ResBody<BatchResult | null>>(`/role/${id}/permissions`, { data });
};

export const listRoleUsers = (
    id: number,
    params: ListRoleUsersParams = {}
): PromiseResBody<Page<AssignedUserRes>> => {
    return request.get<ResBody<Page<AssignedUserRes>>>(`/role/${id}/users`, {
        params,
    });
};

export const getRoleUsers = (
    id: number,
    page: number = 0,
    size: number = 10,
    userUid?: number,
    username?: string,
    nickname?: string
): PromiseResBody<Page<AssignedUserRes>> => {
    return listRoleUsers(id, { page, size, userUid, username, nickname });
};

export const putUserRoles = (id: number, data: AssignUserToRoleReq): Promise<ResBody<BatchResult | null>> => {
    return request.put<ResBody<BatchResult | null>>(`/role/${id}/users`, data);
};

export const deleteUserRoles = (id: number, data: RemoveRoleUsersReq): Promise<ResBody<BatchResult | null>> => {
    return request.delete<ResBody<BatchResult | null>>(`/role/${id}/users`, { data });
};

export const assignUserRoles = (id: number, data: AssignUserToRoleReq): PromiseResBody<BatchResult | null> => {
    return request.post<ResBody<BatchResult | null>>(`/role/${id}/users`, data);
};

export const getRoleAllIds = (): PromiseResBody<OptionResItem<number>[]> => {
    return request.get<ResBody<OptionResItem<number>[]>>(`/role/options`);
}
