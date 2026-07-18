import axios from 'axios'
import JSONBig from 'json-bigint';
import { useAuthStore } from "~/stores/authStore";
import { APIError } from "@waterfun/web-core/src/errors/APIError";
import type { ApiRes } from "@waterfun/web-core/src/types/api/response";

let isRedirectingToLogin = false;

declare module 'axios' {
    interface AxiosRequestConfig {
        meta?: {
            showError?: boolean;
            needAuth?: boolean;
        };
    }
}
const AUTH_SKIP_LIST: string[] = import.meta.env.VITE_AUTH_SKIP_LIST?.split(',') || [];
const jsonBig = JSONBig({ storeAsString: true });


const parseResponseWithBigInt = (data: unknown) => {
    if (typeof data !== 'string' || data.length === 0) return data;
    try {
        return jsonBig.parse(data);
    } catch {
        // Fallback for non-JSON payloads (e.g. HTML/text)
        return data;
    }
};

const service = axios.create({
    baseURL: import.meta.env.VITE_API_BASE,
    timeout: 5000,
    withCredentials: true, //allow credentials and cookies
    transformResponse: [parseResponseWithBigInt]
}) as ApiRes

// request interceptors
service.interceptors.request.use(
    async config => {
        const isAuthSkip = AUTH_SKIP_LIST.some((path: string) => config.url?.includes(path));
        const needAuth = config.meta?.needAuth !== false && !isAuthSkip;

        const token = useAuthStore().accessData.token;
        if (needAuth && token && token.trim().length > 0) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }

        if (config.method == 'GET') {
            config.params = {
                _t: Date.now(),
                _n: Math.random().toString(36).slice(2),
                ...config.params
            }
        }
        return config;
    },
    error => {
        return Promise.reject(error)
    }
)

// response interceptors
service.interceptors.response.use(
    response => {
        if (response.status !== 200) {
            console.error(response);
            return Promise.reject(new Error(response.data.message || 'Error'))
        } else {
            return response.data
        }
    },
    error => {
        if (error?.response) {
            const status: number | undefined = error.response.status;
            const data: unknown = error.response.data;

            const apiError = APIError.fromHttp({
                code: (data as any)?.code,
                httpStatus: status,
                raw: data,
            });
            console.log(data);
            if (status === 401 && (data as any)?.code === 'system.invalid_token_or_expired') {
                useAuthStore().removeToken();
                const isOnLoginPage = window.location.pathname === '/login';
                if (!isOnLoginPage && !isRedirectingToLogin) {
                    isRedirectingToLogin = true;
                    window.location.replace('/login');
                }
            }

            return Promise.reject(apiError);
        }
        if (error?.request) {
            return Promise.reject(APIError.fromHttp({ code: 'general.unknown_error' }));
        }
        return Promise.reject(APIError.fromHttp({ code: 'general.unknown_error' }));
    }
)

export default service;