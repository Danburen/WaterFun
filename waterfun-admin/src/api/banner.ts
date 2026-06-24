import type { ISOString, PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import type { Page } from "~/types/api";
import request from "~/utils/axiosRequest";
import { getUploadPolicy, type PresignedResp } from "~/api/resource";
import type { CloudResPresignedUrlResp } from "~/api/moderation";

export type BannerPosition = "HOME" | "SIDE";
export type BannerStatus = "SHOW" | "HIDE";

export interface BannerResp {
  id: string;
  coverageUrl?: CloudResPresignedUrlResp;
  title: string;
  subtitle?: string;
  linkUrl?: string;
  position?: BannerPosition;
  sortNo?: number;
  status?: BannerStatus;
  startAt?: ISOString;
  endAt?: ISOString;
  createdAt?: ISOString;
  updatedAt?: ISOString;
}

export interface ListBannerParams {
  page?: number;
  size?: number;
  title?: string;
  subtitle?: string;
  position?: BannerPosition;
  status?: BannerStatus;
  startAt?: string;
  endAt?: string;
  isDeleted?: boolean;
}

export interface CreateBannerRequest {
  title: string;
  subtitle?: string;
  linkUrl?: string;
  position?: BannerPosition;
  sortNo?: number;
  status?: BannerStatus;
  startAt?: string | null;
  endAt?: string | null;
  imageUuid: string;
}

export interface UpdateBannerRequest {
  resourceKey?: string;
  title?: string;
  subtitle?: string;
  linkUrl?: string;
  position?: BannerPosition;
  sortNo?: number;
  status?: BannerStatus;
  startAt?: string | null;
  endAt?: string | null;
  imageUuid?: string;
}

export const listBanners = (params: ListBannerParams = {}): PromiseResBody<Page<BannerResp>> => {
  return request.get<Page<BannerResp>>("/content/banner/list", { params });
};

export const getBannerById = (id: string): PromiseResBody<BannerResp> => {
  return request.get<BannerResp>(`/content/banner/${id}`);
};

export const createBanner = (data: CreateBannerRequest): PromiseResBody<void> => {
  return request.post<void>("/content/banner", data);
};

export const updateBanner = (id: string, data: UpdateBannerRequest): PromiseResBody<void> => {
  return request.put<void>(`/content/banner/${id}`, data);
};

export const deleteBanner = (id: string): PromiseResBody<void> => {
  return request.delete<void>(`/content/banner/${id}`);
};

export const getBannerUploadPolicy = (exts: string[], bizId: string = "0"): PromiseResBody<PresignedResp[]> => {
  return getUploadPolicy({
    bizType: "BANNER_IMAGE",
    bizId,
    exts,
  });
};
