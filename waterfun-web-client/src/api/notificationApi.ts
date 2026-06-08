import request from "../utils/axiosRequest";
import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response";

export interface SystemNotificationRes {
  id?: string;
  title: string;
  content: string;
  createdAt: string;
  isRead?: boolean;
}

export interface CursorPageLong<T> {
  list: T[];
  nextCursor: string | null;
  hasNext: boolean;
}

export interface ListSystemNotificationParams {
  cursor?: string;
  limit: number;
  unreadOnly?: boolean;
}

export interface BatchMarkReadReq {
  ids: string[];
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

export const markSystemNotificationReadById = (id: string): PromiseResBody<void> => {
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
