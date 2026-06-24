import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import request from "~/utils/axiosRequest";

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

/** Matches AdminUploadPolicyReq from OpenAPI — admin upload policy request */
export type AdminUploadBizType = 'BANNER_IMAGE' | 'POST_COVERAGE_IMAGE' | 'POST_CONTENT_IMAGE';

export interface UploadPolicyReq {
  bizType: AdminUploadBizType;
  /** Required by admin upload API */
  bizId: string;
  exts: string[];
}

export interface PresignedResp {
  url: string;
  method: HttpMethod;
  token?: string;
  success?: boolean;
  errorMsg?: string;
}

export interface CloudPutCallbackReq {
  token: string;
}

export interface UploadCallbackResp {
  uuid: string;
}

/** Get presigned upload policy from admin endpoint (no moderation review needed) */
export const getUploadPolicy = (data: UploadPolicyReq): PromiseResBody<PresignedResp[]> => {
  return request.post<PresignedResp[]>("/upload/policy", data);
};

/** Upload file directly to COS using presigned URL */
export const uploadFileToStorage = (url: string, method: HttpMethod | string, file: File): Promise<Response> => {
  const uppercaseMethod = method.toUpperCase();
  return fetch(url, {
    method: uppercaseMethod,
    body: file,
    headers: {},
  });
};

/** Confirm upload completion with token */
export const uploadCallback = (data: CloudPutCallbackReq): PromiseResBody<UploadCallbackResp> => {
  return request.post<UploadCallbackResp>("/upload/callback", data);
};

/**
 * Convenience: upload an image via admin policy + callback in one async step.
 * Returns the resource UUID on success, or null on failure.
 */
export async function uploadImage(bizType: AdminUploadBizType, file: File): Promise<string | null> {
  const ext = file.name.split('.').pop() || 'png';
  try {
    const policyResp = await getUploadPolicy({
      bizType,
      bizId: ext,
      exts: [ext],
    });
    const policies = policyResp.data;
    if (!policies?.length) return null;

    const { url, method, token } = policies[0];
    if (!token) return null;

    const uploadResp = await uploadFileToStorage(url, method, file);
    if (!uploadResp.ok) return null;

    const callbackResp = await uploadCallback({ token });
    return callbackResp.data?.uuid ?? null;
  } catch {
    return null;
  }
}
