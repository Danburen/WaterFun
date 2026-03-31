import request from "../utils/axiosRequest"
import type { LoginRequest, AccessTokenResponse } from "@waterfun/web-core/src/types/api/auth";
import type { DataApiResponse, PromiseApiRes } from "@waterfun/web-core/src/types/api/response";
import axios from "axios";

export const login = (data: LoginRequest): PromiseApiRes<AccessTokenResponse> => {
    return request.post('/auth/login-by-password', data);
}
