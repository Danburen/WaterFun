import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import request from "~/utils/axiosRequest";

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

/**
 * Presigned URL response from backend
 * Corresponds to Java PresignedResp DTO
 */
export interface PresignedResp {
  /** path key without prefix */
  key: string;
  /** presigned upload URL */
  url: string;
  /** HTTP method for upload */
  method: HttpMethod;
  /** upload token for callback verification */
  token: string;
}

/**
 * Upload callback request payload
 */
export interface UploadCallbackRequest {
  key: string;
  token: string;
}

/**
 * Get presigned URL for file upload
 * @param suffix - file extension (e.g., 'jpg', 'png', 'pdf')
 * @returns presigned upload policy
 */
export const getPresignedUrl = (suffix: string): PromiseResBody<PresignedResp> => {
  return request.get<PresignedResp>("/resource/presigned", { params: { suffix } });
};

/**
 * Upload file to cloud storage using presigned URL
 * @param url - presigned URL
 * @param method - HTTP method
 * @param file - file to upload
 * @returns fetch response
 */
export const uploadFileToStorage = (url: string, method: HttpMethod | string, file: File): Promise<Response> => {
  const uppercaseMethod = method.toUpperCase();
  const fetchOptions: RequestInit = {
    method: uppercaseMethod,
    body: file,
    headers: {}
  };
  return fetch(url, fetchOptions);
};

/**
 * Callback to notify backend that upload is complete
 * @param data - callback payload with key and token
 * @returns void response
 */
export const uploadCallback = (data: UploadCallbackRequest): PromiseResBody<null> => {
  return request.post<null>("/resource/upload/callback", data);
};
