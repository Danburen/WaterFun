import type { ISOString, PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import type { Page, BatchResult } from "~/types/api";
import request from "~/utils/axiosRequest";

export type PostStatus = "DRAFT" | "PENDING" | "PUBLISHED" | "REJECTED" | "ARCHIVED";
export type PostVisibility = "PUBLIC" | "PRIVATE" | "FANS_ONLY";
export type PostType = "COMMON" | "NOTICE";

/** Matches PostResponse from OpenAPI — response from GET /api/admin/posts/{id} */
export interface PostResp {
  id: string;
  title: string;
  subtitle?: string;
  content?: string;
  contentHtml?: string;
  summary?: string;
  authorId: string;
  coverImg?: string;
  coverImage?: { url: string; expireAt: string } | null;
  status?: PostStatus;
  visibility?: PostVisibility;
  categoryId?: string;
  tagIds?: string[];
  viewCount?: string;
  likeCount?: string;
  commentCount?: string;
  collectCount?: string;
  slug?: string;
  type?: PostType;
  isPinned?: boolean;
  publishedAt?: ISOString;
  createdAt?: ISOString;
  updatedAt?: ISOString;
  editedTitle?: string;
  editedSubtitle?: string;
  editedContent?: string;
  editedContentHtml?: string;
  editedSummary?: string;
  editedCoverImg?: string;
  editedCategoryId?: string;
  editedTagIds?: string[];
  isAnnouncement?: boolean;
}

export interface ListPostParams {
  page?: number;
  size?: number;
  title?: string;
  status?: PostStatus;
  categoryId?: string;
  authorId?: string;
  tagIds?: string[];
  slug?: string;
}

/** Matches PutPostReq from OpenAPI — used for both create and update */
export interface UpsertPostRequest {
  title: string;
  content: string;
  subtitle?: string;
  summary?: string;
  /** UUID returned from upload callback, not a URL */
  coverageUuid?: string;
  status: PostStatus;
  visibility: PostVisibility;
  authorId?: string;
  categoryId?: string;
  slug?: string;
  tagIds?: string[];
  type?: PostType;
  isPinned?: boolean;
  isAnnouncement?: boolean;
}

export interface DeletePostRequest {
  postIds: string[];
}

export interface AssignTagsRequest {
  tagIds: string[];
}

export const previewContent = (content: string): PromiseResBody<string> => {
  return request.post<string>("/posts/content/preview", content, {
    headers: { 'Content-Type': 'text/plain' },
    meta: { needCSRF: false },
  });
};

export const listPosts = (params: ListPostParams = {}): PromiseResBody<Page<PostResp>> => {
  return request.get<Page<PostResp>>("/posts/list", { params });
};

export const getPostById = (id: string | number): PromiseResBody<PostResp> => {
  return request.get<PostResp>(`/posts/${id}`);
};

export const createPost = (data: UpsertPostRequest): PromiseResBody<null> => {
  return request.post<null>("/posts", data);
};

export const putPost = (id: string | number, data: UpsertPostRequest): PromiseResBody<null> => {
  return request.put<null>(`/posts/${id}`, data);
};

export const deletePostById = (id: string | number): PromiseResBody<null> => {
  return request.delete<null>(`/posts/${id}`);
};

export const deletePosts = (postIds: Array<string | number>): PromiseResBody<BatchResult> => {
  const data: DeletePostRequest = { postIds: postIds.map((id) => String(id)) };
  return request.delete<BatchResult>("/posts", { data });
};

export const assignPostTags = (id: string | number, data: AssignTagsRequest): PromiseResBody<BatchResult> => {
  return request.post<BatchResult>(`/posts/${id}/tags`, data);
};

export const replacePostTags = (id: string | number, data: AssignTagsRequest): PromiseResBody<BatchResult> => {
  return request.put<BatchResult>(`/posts/${id}/tags`, data);
};

export const removePostTags = (id: string | number, data: AssignTagsRequest): PromiseResBody<BatchResult> => {
  return request.delete<BatchResult>(`/posts/${id}/tags`, { data });
};
