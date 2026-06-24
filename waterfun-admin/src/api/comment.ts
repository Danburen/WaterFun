import type { ISOString, PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import type { Page } from "~/types/api";
import request from "~/utils/axiosRequest";

export type ReportType = "VIOLATION_OF_GUIDELINES" | "INAPPROPRIATE_CONTENT" | "ADVERTISEMENT" | "VIOLENCE" | "SENSITIVE" | "CASCADE" | "OTHER";

export interface CommentResponse {
  id: number;
  postId: number;
  authorId: number;
  content: string;
  parentId: number | null;
  rootId: number | null;
  likeCount: number;
  replyCount: number;
  status: string;
  createdAt: ISOString;
  updatedAt: ISOString;
}

export interface CursorPageCommentResponse {
  list: CommentResponse[];
  nextCursor: string | number | null;
  hasNext: boolean;
}

export interface CreateReportReq {
  type: ReportType;
  reason?: string;
}

export const createComment = (data: {
  postId: number;
  parentId?: number;
  content: string;
}): PromiseResBody<null> => {
  return request.post<null>("/comments", data);
};

export const listPostComments = (params: {
  postId: number;
  cursor?: string;
  limit?: number;
  includeRootId?: number;
}): PromiseResBody<CursorPageCommentResponse> => {
  return request.get<CursorPageCommentResponse>("/comments/list", { params });
};

export const getComment = (commentId: number): PromiseResBody<CommentResponse> => {
  return request.get<CommentResponse>(`/comments/${commentId}`);
};

export const deleteComment = (id: number): PromiseResBody<null> => {
  return request.delete<null>(`/comments/${id}`);
};

export const listCommentReplies = (rootId: number, params: {
  cursor?: number;
  limit?: number;
  includeRootId?: number;
} = {}): PromiseResBody<CursorPageCommentResponse> => {
  return request.get<CursorPageCommentResponse>(`/comments/${rootId}/replies`, { params });
};

export const reportComment = (id: number, data: CreateReportReq): PromiseResBody<{ taskId: number }> => {
  return request.post(`/comments/${id}/report`, data);
};

export const cancelReportComment = (id: number): PromiseResBody<null> => {
  return request.post<null>(`/comments/${id}/report/cancel`);
};

export const likeComment = (id: number): PromiseResBody<null> => {
  return request.post<null>(`/comments/${id}/like`);
};
