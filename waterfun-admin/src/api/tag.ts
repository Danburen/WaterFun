import type { ISOString, OptionResItem, PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import type { Page, BatchResult } from "~/types/api";
import request from "~/utils/axiosRequest";

export type TagOptionVO = OptionResItem<number>;

export interface TagResp {
  id: number;
  name: string;
  slug?: string;
  description?: string;
  usageCount?: number;
  creatorId?: number;
  createdAt?: ISOString;
  updateAt?: ISOString;
}

export interface ListTagParams {
  page?: number;
  size?: number;
  name?: string;
  slug?: string;
  creatorId?: string;
  createdStart?: ISOString;
  createdEnd?: ISOString;
}

export interface UpdateTagRequest {
  name?: string;
  slug?: string;
  description?: string;
}

export interface CreateTagRequest {
  name: string;
  slug?: string;
  description?: string;
}

export interface DeleteTagsRequest {
  tagIds: number[];
}

export const listTags = (params: ListTagParams = {}): PromiseResBody<Page<TagResp>> => {
  return request.get<Page<TagResp>>("/tags/list", { params });
};

export const getTag = (id: number): PromiseResBody<TagResp> => {
  return request.get<TagResp>(`/tags/${id}`);
};

export const createTag = (data: CreateTagRequest): PromiseResBody<null> => {
  return request.post<null>("/tags", data);
};

export const putTag = (id: number, data: UpdateTagRequest): PromiseResBody<null> => {
  return request.put<null>(`/tags/${id}`, data);
};

export const deleteTag = (id: number): PromiseResBody<null> => {
  return request.delete<null>(`/tags/${id}`);
};

export const deleteTags = (tagIds: number[]): PromiseResBody<BatchResult> => {
  const data: DeleteTagsRequest = { tagIds };
  return request.delete<BatchResult>("/tags", { data });
};

export const getTagOptions = (): PromiseResBody<OptionResItem<number>[]> => {
  return request.get<OptionResItem<number>[]>("/tags/options");
};

export const searchTags = (keyword: string, limit: number = 10): PromiseResBody<OptionResItem<number>[]> => {
  return request.get<OptionResItem<number>[]>("/tags/search", { params: { keyword, limit } });
};

export const getHotTags = (limit: number = 20): PromiseResBody<OptionResItem<number>[]> => {
  return request.get<OptionResItem<number>[]>("/tags/hot", { params: { limit } });
};
