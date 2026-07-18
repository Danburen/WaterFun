import type { ISOString, PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import type { Page } from "~/types/api";
import request from "~/utils/axiosRequest";

export interface IpBanResponse {
  id: string;
  ip: string;
  reason?: string;
  bannedAt: ISOString;
  expiresAt: ISOString;
}

export interface BanIpRequest {
  ip: string;
  reason?: string;
  expiresAt?: ISOString;
}

export interface UnbanIpRequest {
  ip: string;
}

export interface ListIpBanParams {
  page?: number;
  size?: number;
  ip?: string;
  reason?: string;
  bannedAtStart?: ISOString;
  bannedAtEnd?: ISOString;
  expiresStart?: ISOString;
  expiresEnd?: ISOString;
}

export interface AuditLogResponse {
  id: string;
  userId?: string;
  username?: string;
  action: "UNKNOWN" | "LOGIN" | "REGISTER" | "CHANGE_PASSWORD";
  ip?: string;
  deviceInfo?: Record<string, unknown>;
  country?: string;
  province?: string;
  city?: string;
  status: "FAIL" | "SUCCESS";
  failReason?: string;
  createdAt: ISOString;
}

export interface ListAuditLogParams {
  page?: number;
  size?: number;
  userId?: string;
  username?: string;
  action?: "UNKNOWN" | "LOGIN" | "REGISTER" | "CHANGE_PASSWORD";
  ip?: string;
  status?: "FAIL" | "SUCCESS";
  createdAtStart?: ISOString;
  createdAtEnd?: ISOString;
}

export interface DeleteAuditLogRequest {
  logIds: number[];
}

export const listIpBans = (params: ListIpBanParams = {}): PromiseResBody<Page<IpBanResponse>> => {
  return request.get<Page<IpBanResponse>>("/security/list", { params });
};

export const getIpBan = (id: number): PromiseResBody<IpBanResponse> => {
  return request.get<IpBanResponse>(`/security/ban/${id}`);
};

export const banIp = (data: BanIpRequest): PromiseResBody<IpBanResponse> => {
  return request.post<IpBanResponse>("/security/ban", data);
};

export const unbanIp = (data: UnbanIpRequest): PromiseResBody<null> => {
  return request.post<null>("/security/unban", data);
};

export const deleteIpBan = (id: number): PromiseResBody<null> => {
  return request.delete<null>(`/security/ban/${id}`);
};

export const listAuditLogs = (params: ListAuditLogParams = {}): PromiseResBody<Page<AuditLogResponse>> => {
  return request.get<Page<AuditLogResponse>>("/security/audit-log/list", { params });
};

export const getAuditLog = (id: number): PromiseResBody<AuditLogResponse> => {
  return request.get<AuditLogResponse>(`/security/audit-log/${id}`);
};

export const deleteAuditLog = (id: number): PromiseResBody<null> => {
  return request.delete<null>(`/security/audit-log/${id}`);
};

export const deleteAuditLogs = (logIds: number[]): PromiseResBody<null> => {
  return request.delete<null>("/security/audit-log/list", { data: { logIds } });
};

// ==================== IP 访问日志 ====================

export interface IpAccessLogResponse {
  id: string;
  ip: string;
  requestPath: string;
  requestMethod: string;
  userUid?: string;
  httpStatus?: number;
  country?: string;
  province?: string;
  city?: string;
  createdAt: ISOString;
}

export interface ListIpAccessLogParams {
  page?: number;
  size?: number;
  ip?: string;
  userUid?: string;
  requestPath?: string;
  requestMethod?: string;
  httpStatus?: number;
  country?: string;
  province?: string;
  city?: string;
  createdAtStart?: ISOString;
  createdAtEnd?: ISOString;
}

export const listIpAccessLogs = (params: ListIpAccessLogParams = {}): PromiseResBody<Page<IpAccessLogResponse>> => {
  return request.get<Page<IpAccessLogResponse>>("/security/access-log/list", { params });
};

export const getIpAccessLog = (id: number): PromiseResBody<IpAccessLogResponse> => {
  return request.get<IpAccessLogResponse>(`/security/access-log/${id}`);
};
