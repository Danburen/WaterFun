import axios from 'axios'
import JSONBig from 'json-bigint';
import { useAuthStore } from "~/stores/authStore";
import { generateFingerprint } from "@waterfun/web-core/src/fingerprint";
import { APIError } from "@waterfun/web-core/src/errors/APIError";
import type { ApiRes } from "@waterfun/web-core/src/types/api/response";

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

/** 专用于 token 刷新的 axios 实例（无 response 拦截器，避免循环） */
const refreshService = axios.create({
    baseURL: import.meta.env.VITE_API_BASE,
    timeout: 5000,
    withCredentials: true,
    transformResponse: [parseResponseWithBigInt]
})

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

// response interceptors — with refresh token rotation
/** 共享的 refresh Promise：所有并发 401 等待同一个，消除 race condition */
let refreshPromise: Promise<boolean> | null = null;

async function doRefresh(): Promise<boolean> {
    try {
        const deviceFp = await generateFingerprint();
        const res = await refreshService.post('/auth/refresh', null, {
            params: { deviceFp },
        });
        const data = res.data as any;
        const newAccess = data.data.accessToken;
        const newExp = Date.now() + data.data.exp * 1000;
        useAuthStore().setToken(newAccess, newExp);
        return true;
    } catch (err) {
        console.error('Token refresh failed', err);
        useAuthStore().removeToken();
        return false;
    }
}

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
            const body = (data as any) || {};

            const apiError = APIError.fromHttp({
                code: body.code,
                httpStatus: status,
                raw: data,
            });
            console.log(data);

            if (status === 401 && body.code === 'system.invalid_token_or_expired') {
                const authStore = useAuthStore();
                const originalRequest = error.config;

                // Don't retry refresh endpoint itself
                if (originalRequest?.url?.includes('/auth/refresh')) {
                    authStore.removeToken();
                    if (window.location.pathname !== '/login') {
                        window.location.replace('/login');
                    }
                    return Promise.reject(apiError);
                }

                const isOnLoginPage = window.location.pathname === '/login';
                if (isOnLoginPage) {
                    return Promise.reject(apiError);
                }

                if (!authStore.accessData.token) {
                    authStore.removeToken();
                    if (window.location.pathname !== '/login') {
                        window.location.replace('/login');
                    }
                    return Promise.reject(apiError);
                }

                // 共享 Promise：所有 401 等待同一个 refresh
                if (!refreshPromise) {
                    refreshPromise = doRefresh();
                }
                const captured = refreshPromise;

                return captured.then(success => {
                    if (!success) {
                        if (window.location.pathname !== '/login') {
                            window.location.replace('/login');
                        }
                        return Promise.reject(apiError);
                    }
                    const token = authStore.accessData.token;
                    if (!token) {
                        if (window.location.pathname !== '/login') {
                            window.location.replace('/login');
                        }
                        return Promise.reject(apiError);
                    }
                    originalRequest.headers['Authorization'] = `Bearer ${token}`;
                    return service(originalRequest);
                }).finally(() => {
                    if (refreshPromise === captured) {
                        refreshPromise = null;
                    }
                });
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