import request from "../utils/axiosRequest"
import type { LoginRequest, AccessTokenResponse } from "@waterfun/web-core/src/types/api/auth";
import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response";

export const login = (data: LoginRequest): PromiseResBody<AccessTokenResponse> => {
    return request.post('/auth/login-by-password', data);
}

export const logout = (deviceFp: string): PromiseResBody<void> => {
    return request.post('/auth/logout', deviceFp);
}
