import request from "../utils/axiosRequest";
import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response";

export interface InstantDto {
  seconds: number;
  nanos: number;
}

export interface SystemNotificationRes {
  id?: number;
  title: string;
  content: string;
  createdAt: InstantDto;
  isRead?: boolean;
}

export interface CursorPageLong<T> {
  list: T[];
  nextCursor: number | null;
  hasNext: boolean;
}

export interface ListSystemNotificationParams {
  cursor?: number;
  limit: number;
  unreadOnly?: boolean;
}

export interface BatchMarkReadReq {
  ids: number[];
}

export interface BatchResult {
  requested: number;
  success: number;
  ignored: number;
  failed: number;
}

export const listSystemNotifications = (
  params: ListSystemNotificationParams
): PromiseResBody<CursorPageLong<SystemNotificationRes>> => {
  return request.get("/notifications/system/list", { params });
};

export const getSystemNotificationUnreadCount = (): PromiseResBody<number> => {
  return request.get("/notifications/system/unreadCount");
};

export const markSystemNotificationReadById = (id: number): PromiseResBody<void> => {
  return request.post(`/notifications/system/${id}/batchMarkRead`);
};

export const markAllSystemNotificationsRead = (): PromiseResBody<void> => {
  return request.post("/notifications/system/markAllRead");
};

export const batchMarkSystemNotificationsRead = (
  data: BatchMarkReadReq
): PromiseResBody<BatchResult> => {
  return request.post("/notifications/system/markRead", data);
};
