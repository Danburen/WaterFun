import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import request from "~/utils/axiosRequest";

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

export type AdminUploadBizType = 'BANNER_IMAGE';

export interface UploadPolicyReq {
  bizType: AdminUploadBizType;
  bizId: string;
  exts: string[];
}

export interface PresignedResp {
  url: string;
  method: HttpMethod;
  token: string;
  success?: boolean;
  errorMsg?: string;
}

export interface CloudPutCallbackReq {
  token: string;
}

export interface UploadCallbackResp {
  uuid: string;
}

export const getUploadPolicy = (data: UploadPolicyReq): PromiseResBody<PresignedResp[]> => {
  return request.post<PresignedResp[]>("/upload/policy", data);
};

export const uploadFileToStorage = (url: string, method: HttpMethod | string, file: File): Promise<Response> => {
  const uppercaseMethod = method.toUpperCase();
  const fetchOptions: RequestInit = {
    method: uppercaseMethod,
    body: file,
    headers: {}
  };
  return fetch(url, fetchOptions);
};

export const uploadCallback = (data: CloudPutCallbackReq): PromiseResBody<UploadCallbackResp> => {
  return request.post<UploadCallbackResp>("/upload/callback", data);
};
