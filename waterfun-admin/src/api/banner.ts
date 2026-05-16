import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import type { CloudResPresignedUrlResp } from "@waterfun/web-core/src/types/api/response";
import type { Page } from "~/types/api";
import request from "~/utils/axiosRequest";

export type BannerPosition = "HOME" | "SIDE";
export type BannerStatus = "SHOW" | "HIDE";
import type { OptionResItem, PromiseResBody } from "@waterfun/web-core/src/types/api/response";
export interface Instant {
  seconds: number;
  nanos: number;
}

export interface BannerResp {
  id: number;
  coverageUrl?: CloudResPresignedUrlResp;
  title: string;
  subtitle?: string;
  linkUrl?: string;
  position?: BannerPosition;
  sortNo?: number;
  status?: BannerStatus;
  startAt?: Instant | string | null;
  endAt?: Instant | string | null;
  createdAt?: Instant | string | null;
  updatedAt?: Instant | string | null;
}

export interface ListBannerParams {
  page?: number;
  size?: number;
  title?: string;
  subtitle?: string;
  position?: BannerPosition;
  status?: BannerStatus;
}

export interface CloudPutCallbackReq {
  key: string;
  token: string;
}

export interface CreateBannerRequest {
  title: string;
  subtitle?: string;
  linkUrl?: string;
  position?: BannerPosition;
  sortNo?: number;
  status?: BannerStatus;
  startAt?: Instant;
  endAt?: Instant;
  putCallback: CloudPutCallbackReq;
}

export interface UpdateBannerRequest {
  resourceKey?: string;
  title?: string;
  subtitle?: string;
  linkUrl?: string;
  position?: BannerPosition;
  sortNo?: number;
  status?: BannerStatus;
  startAt?: Instant;
  endAt?: Instant;
}

export interface CoverageUploadResp {
  key: string;
  url: string;
  method: "GET" | "POST" | "PUT" | "DELETE" | "PATCH";
  token: string;
}

export const listBanners = (params: ListBannerParams = {}): PromiseResBody<Page<BannerResp>> => {
  return request.get<Page<BannerResp>>("/content/banner/list", { params });
};

export const getBannerById = (id: number): PromiseResBody<BannerResp> => {
  return request.get<BannerResp>(`/content/banner/${id}`);
};

export const createBanner = (data: CreateBannerRequest): PromiseResBody<string> => {
  return request.post<string>("/content/banner", data);
};

export const updateBanner = (id: number, data: UpdateBannerRequest): PromiseResBody<string> => {
  return request.put<string>(`/content/banner/${id}`, data);
};

export const getBannerCoverageUpload = (suffix: string): PromiseResBody<CoverageUploadResp> => {
  return request.get<CoverageUploadResp>("/content/banner/coverage/upload", { params: { suffix } });
};
