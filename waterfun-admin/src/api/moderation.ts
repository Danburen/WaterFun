import type { ISOString, PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import type { Page } from "~/types/api";
import request from "~/utils/axiosRequest";

export type TargetType =
  | "UNKNOWN"
  | "USER_AVATAR"
  | "POST"
  | "POST_COVERAGE_IMAGE"
  | "POST_CONTENT_IMAGE"
  | "BANNER_IMAGE"
  | "POST_CONTENT";

export type ModerateRejectType =
  | "VIOLATION_OF_GUIDELINES"
  | "INAPPROPRIATE_CONTENT"
  | "ADVERTISEMENT"
  | "VIOLENCE"
  | "SENSITIVE"
  | "CASCADE"
  | "OTHER";

export type AuditStatus = "PENDING" | "APPROVED" | "REJECTED" | "SUSPECT";

export type ResourceType = "UNKNOWN" | "IMAGE" | "VIDEO" | "AUDIO" | "TEXT" | "DOCUMENT" | "ARCHIVE" | "EXECUTABLE" | "OTHER";

export type PayloadType = "SINGLE_RESOURCE" | "RICH_TEXT" | "PLAIN_TEXT";

export type AuditContentFormat = "DEFAULT" | "PLAINTEXT" | "HTML" | "MARKDOWN";

export interface PostAuditPayload {
  title?: string;
  subTitle?: string;
  content?: string;
  summary?: string;
  coverageResUuid?: string;
  categoryId?: number;
  tagIds?: number[];
  newTagNames?: string[];
}

export interface FileMeta {
  eTag?: string;
}

export interface FileProbeResult {
  size?: string;
  mimeType?: string;
  meta?: FileMeta;
}

export interface CloudResPresignedUrlResp {
  url?: string;
  expireAt?: ISOString;
}

export interface ModerationResourceRes {
  taskId?: string;
  resourceUuid?: string;
  status?: AuditStatus;
  auditAt?: ISOString;
  auditorId?: string;
  rejectType?: ModerateRejectType;
  rejectReason?: string;
  fileProbeResult?: FileProbeResult | null;
  presignedUrl?: CloudResPresignedUrlResp | null;
}

export interface ModerationTaskPayloadRes {
  type?: PayloadType;
  resources?: ModerationResourceRes[];
  content?: string;
  contentFormat?: AuditContentFormat;
  meta?: Record<string, unknown>;
}

export interface ModerateTaskResp {
  id?: string;
  targetType?: TargetType;
  targetId?: string;
  payload?: ModerationTaskPayloadRes;
  submitterId?: string;
  submitAt?: ISOString;
}

export interface BatchResult {
  requested: number;
  success: number;
  ignored: number;
  failed: number;
}

export interface ListModerationParams {
  page?: number;
  size?: number;
  taskType?: TargetType;
  submitterId?: string;
  submitAtStart?: string;
  submitAtEnd?: string;
}

export interface ListModerationResourceParams {
  page?: number;
  size?: number;
  taskId?: string;
  status?: AuditStatus;
  resourceType?: ResourceType;
  auditorId?: string;
  auditAtStart?: string;
  auditAtEnd?: string;
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

export const getModerationTaskById = (id: string): PromiseResBody<ModerateTaskResp> => {
  return request.get<ModerateTaskResp>(`/moderations/${id}`);
};

export const listModerations = (params: ListModerationParams): PromiseResBody<Page<ModerateTaskResp>> => {
  return request.get<Page<ModerateTaskResp>>("/moderations/list", { params });
};

export const approveModerationById = (id: string): PromiseResBody<BatchResult> => {
  return request.post<BatchResult>(`/moderations/${id}/approve`);
};

export const rejectModerationById = (id: string, data: ModerateRejectRequest): PromiseResBody<BatchResult> => {
  return request.post<BatchResult>(`/moderations/${id}/reject`, data);
};

export const approveModerations = (data: BatchModerateRequest): PromiseResBody<BatchResult> => {
  return request.post<BatchResult>("/moderations/approve", data);
};

export const rejectModerations = (data: BatchModerateRejectRequest): PromiseResBody<BatchResult> => {
  return request.post<BatchResult>("/moderations/reject", data);
};

export const listResourcesByTask = (id: string): PromiseResBody<ModerationResourceRes[]> => {
  return request.get<ModerationResourceRes[]>(`/moderations/${id}/resources`);
};

export const listModerationResources = (
  params: ListModerationResourceParams
): PromiseResBody<Page<ModerationResourceRes>> => {
  return request.get<Page<ModerationResourceRes>>("/moderations/resources/list", { params });
};

export const getModerationResource = (taskId: string, resourceUuid: string): PromiseResBody<ModerationResourceRes> => {
  return request.get<ModerationResourceRes>(`/moderations/${taskId}/resources/${resourceUuid}`);
};

export const approveModerationResource = (taskId: string, resourceUuid: string): PromiseResBody<void> => {
  return request.post<void>(`/moderations/${taskId}/resources/${resourceUuid}/approve`);
};

export const rejectModerationResource = (
  taskId: string,
  resourceUuid: string,
  data: ModerateRejectRequest
): PromiseResBody<void> => {
  return request.post<void>(`/moderations/${taskId}/resources/${resourceUuid}/reject`, data);
};
