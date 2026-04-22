import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import type { Page } from "~/types/api";
import request from "~/utils/axiosRequest";

export type PostStatus = "DRAFT" | "PENDING" | "PUBLISHED" | "REJECTED" | "ARCHIVED";
export type PostVisibility = "PUBLIC" | "PRIVATE" | "FANS_ONLY";

export interface Instant {
  seconds: number;
  nanos: number;
}

export interface PostResp {
  id: string;
  title: string;
  subtitle?: string;
  content?: string;
  summary?: string;
  coverImg?: string;
  status?: PostStatus;
  visibility?: PostVisibility;
  categoryId: number;
  authorId: string;
  tagIds?: number[];
  viewCount?: number;
  likeCount?: number;
  commentCount?: number;
  collectCount?: number;
  slug?: string;
  publishedAt?: Instant | string | null;
  createdAt?: Instant | string | null;
  updatedAt?: Instant | string | null;
}

export interface ListPostParams {
  page?: number;
  size?: number;
  title?: string;
  status?: PostStatus;
  categoryId?: number;
  authorId?: string;
  tagIds?: number[];
  slug?: string;
}

export interface CreatePostRequest {
  title: string;
  content: string;
  subtitle?: string;
  summary?: string;
  coverImg?: string;
  status: PostStatus;
  visibility: PostVisibility;
  authorId: string;
  categoryId: number;
  slug?: string;
  tagIds?: number[];
}

export interface UpdatePostRequest {
  title: string;
  content: string;
  subtitle?: string;
  summary?: string;
  coverImg?: string;
  status?: PostStatus;
  visibility?: PostVisibility;
  authorId?: string;
  categoryId?: number;
  slug?: string;
}

export interface DeletePostRequest {
  postIds: string[];
}

export interface AssignTagsRequest {
  tagIds?: number[];
}

export interface BatchResult {
  requested: number;
  success: number;
  ignored: number;
  failed: number;
}

export const listPosts = (params: ListPostParams = {}): PromiseResBody<Page<PostResp>> => {
  return request.get<Page<PostResp>>("/posts/list", { params });
};

export const getPostById = (id: string | number): PromiseResBody<PostResp> => {
  return request.get<PostResp>("/posts/id", { params: { id } });
};

export const createPost = (data: CreatePostRequest): PromiseResBody<null> => {
  return request.post<null>("/posts", data);
};

export const putPost = (id: string | number, data: UpdatePostRequest): PromiseResBody<null> => {
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
