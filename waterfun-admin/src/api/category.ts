import type { OptionResItem, PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import type { Page } from "~/types/api";
import request from "~/utils/axiosRequest";

export interface Instant {
  seconds: number;
  nanos: number;
}

export interface CategoryResp {
  id: number;
  name: string;
  slug?: string;
  description?: string;
  parentId?: number;
  sortOrder?: number;
  isActive?: boolean;
  creatorId?: string;
  updateAt?: Instant | string | null;
  createdAt?: Instant | string | null;
}

export interface ListCategoryParams {
  page?: number;
  size?: number;
  name?: string;
  slug?: string;
  parentId?: number;
  creatorId?: string;
}

export interface UpdateCategoryRequest {
  name?: string;
  slug?: string;
  description?: string;
  parentId?: number;
  sortOrder?: number;
  isActive?: boolean;
}

export interface CreateCategoryRequest {
  name: string;
  slug?: string;
  description?: string;
  parentId?: number;
  sortOrder?: number;
  isActive?: boolean;
}

export interface RemoveCategoriesRequest {
  categoryIds: number[];
}

export interface BatchResult {
  requested: number;
  success: number;
  ignored: number;
  failed: number;
}

export const listCategories = (params: ListCategoryParams = {}): PromiseResBody<Page<CategoryResp>> => {
  return request.get<Page<CategoryResp>>("/categories/list", { params });
};

export const getCategory = (id: number): PromiseResBody<CategoryResp> => {
  return request.get<CategoryResp>(`/categories/${id}`);
};

export const createCategory = (data: CreateCategoryRequest): PromiseResBody<null> => {
  return request.post<null>("/categories", data);
};

export const putCategory = (id: number, data: UpdateCategoryRequest): PromiseResBody<null> => {
  return request.put<null>(`/categories/${id}`, data);
};

export const deleteCategory = (id: number): PromiseResBody<null> => {
  return request.delete<null>(`/categories/${id}`);
};

export const deleteCategories = (categoryIds: number[]): PromiseResBody<BatchResult> => {
  const data: RemoveCategoriesRequest = { categoryIds };
  return request.delete<BatchResult>("/categories", { data });
};

export const getCategoryOptions = (): PromiseResBody<OptionResItem<number>[]> => {
  return request.get<OptionResItem<number>[]>("/categories/options");
};
