import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import type { Page } from "~/types/api";
import request from "~/utils/axiosRequest";

export type ModerateTaskType = "IMAGE" | "TEXT" | "RICH" | "URL" | "FILE";
export type ModerateRejectType =
  | "VIOLATION_OF_GUIDELINES"
  | "INAPPROPRIATE_CONTENT"
  | "ADVERTISEMENT"
  | "VIOLENCE"
  | "SENSITIVE"
  | "OTHER";
export type AuditStatus = "PENDING" | "APPROVED" | "REJECTED";
export type AuditResourceType = "UNKNOWN" | "IMAGE" | "VIDEO" | "AUDIO" | "OTHER";

export interface SimpleCloudObject {
  key?: string;
  type?: string;
  size?: string | number;
}

export interface ModerateTaskResp {
  id?: string;
  taskType: ModerateTaskType;
  targetId: string;
  content?: SimpleCloudObject | string | null;
  submitterId: string;
  submitAt?: string | null;
}

export interface ListModerationParams {
  page?: number;
  size?: number;
  taskType: ModerateTaskType;
  submitterId: string;
  submitAtStart?: string;
  submitAtEnd?: string;
}

export interface BatchModerateRequest {
  auditTaskIds: string[];
}

export interface BatchModerateRejectRequest {
  auditTaskIds: string[];
  rejectType: ModerateRejectType;
  rejectReason?: string;
}

export interface ModerateRejectRequest {
  rejectType: ModerateRejectType;
  rejectReason?: string;
}

export interface BatchResult {
  requested: number;
  success: number;
  ignored: number;
  failed: number;
}

export interface InstantLike {
  seconds?: number;
  nanos?: number;
}

export interface CloudResPresignedUrlResp {
  url?: string;
  expireAt?: InstantLike | string | null;
}

export interface ModerationResourceResp {
  id?: string;
  taskId?: string;
  placeholder?: string;
  resourceKey?: string;
  resourceType?: AuditResourceType;
  mimeType?: string;
  sizeBytes?: number | string;
  sortNo?: number;
  status?: AuditStatus;
  auditAt?: InstantLike | string | null;
  auditorId?: string;
  rejectType?: ModerateRejectType;
  rejectReason?: string;
  presignedUrl?: CloudResPresignedUrlResp | null;
}

export interface SpringPage<T> {
  content: T[];
  totalElements?: number;
  totalPages?: number;
  number?: number;
  size?: number;
}

export interface ListModerationResourceParams {
  page?: number;
  size?: number;
  taskId?: string;
  status?: AuditStatus;
  resourceType?: AuditResourceType;
  auditorId?: string;
  auditAtStart?: string;
  auditAtEnd?: string;
}

export const listModerations = (params: ListModerationParams): PromiseResBody<Page<ModerateTaskResp>> => {
  return request.get<Page<ModerateTaskResp>>("/moderations/list", { params });
};

export const approveModerations = (data: BatchModerateRequest): PromiseResBody<BatchResult> => {
  return request.post<BatchResult>("/moderations/approve", data);
};

export const rejectModerations = (data: BatchModerateRejectRequest): PromiseResBody<BatchResult> => {
  return request.post<BatchResult>("/moderations/reject", data);
};

export const approveModerationById = (id: string): PromiseResBody<BatchResult> => {
  return request.post<BatchResult>(`/moderations/${id}/approve`);
};

export const rejectModerationById = (id: string, data: ModerateRejectRequest): PromiseResBody<BatchResult> => {
  return request.post<BatchResult>(`/moderations/${id}/reject`, data);
};

export const listModerationResources = (
  params: ListModerationResourceParams
): PromiseResBody<SpringPage<ModerationResourceResp>> => {
  return request.get<SpringPage<ModerationResourceResp>>("/moderations/resources/list", { params });
};

export const getModerationResource = (resourceId: string): PromiseResBody<ModerationResourceResp> => {
  return request.get<ModerationResourceResp>(`/moderations/resources/${resourceId}`);
};

export const approveModerationResource = (resourceId: string): PromiseResBody<void> => {
  return request.post<void>(`/moderations/resources/${resourceId}/approve`);
};

export const rejectModerationResource = (
  resourceId: string,
  data: ModerateRejectRequest
): PromiseResBody<void> => {
  return request.post<void>(`/moderations/resources/${resourceId}/reject`, data);
};
