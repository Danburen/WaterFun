import { getUploadPolicy, uploadFileToStorage, uploadCallback, type AdminUploadBizType, type HttpMethod } from "./resource";

function getFileExt(file: File): string {
  const name = file.name;
  const dot = name.lastIndexOf('.');
  return dot !== -1 ? name.slice(dot + 1) : '';
}

export type { AdminUploadBizType };

/**
 * Upload an image via COS presigned URL pattern:
 * 1. Get presigned upload URL from backend
 * 2. Upload file directly to COS
 * 3. Confirm upload via callback
 * @returns UUID of the uploaded image, or null on failure
 */
export async function uploadImage(bizType: AdminUploadBizType, file: File): Promise<string | null> {
  try {
    const ext = getFileExt(file);

    const policyRes = await getUploadPolicy({
      bizType,
      bizId: "",
      exts: ext ? [ext] : ['jpg', 'png', 'gif', 'webp'],
    });

    if (!policyRes?.data?.length) return null;

    const presigned = policyRes.data[0];
    const uploadRes = await uploadFileToStorage(presigned.url, presigned.method as HttpMethod, file);

    if (!uploadRes.ok) return null;

    const callbackRes = await uploadCallback({ token: presigned.token ?? '' });
    return callbackRes?.data?.uuid ?? null;
  } catch {
    return null;
  }
}
