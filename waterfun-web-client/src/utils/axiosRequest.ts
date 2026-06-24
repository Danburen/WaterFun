import axios from 'axios'
import { ElMessage } from "element-plus";
import { translate } from "~/utils/translator";
import {getErrorMessage} from "~/utils/errorMessage";
import {useAuthStore} from "~/stores/authStore";
import {useUserInfoStore} from "~/stores/userInfoStore";
import type {ApiRes} from "@waterfun/web-core/src/types";
import JSONBig from 'json-bigint'

declare module 'axios' {
    interface AxiosRequestConfig {
        meta?: {
            needCSRF?: boolean;
            needAuth?: boolean;
        };
    }
}

const JSONBigParser = JSONBig({ useNativeBigInt: true, alwaysParseAsBig: true })

function isIdField(key: string): boolean {
  return /^(id|uid|postId|parentId|rootId|commentId|categoryId|tagId|authorId|bizId)$/i.test(key)
}

function normalizeTypes(obj: any): any {
  if (obj === null || obj === undefined) return obj

  if (typeof obj === 'bigint') {
    if (obj > BigInt(Number.MAX_SAFE_INTEGER) || obj < BigInt(Number.MIN_SAFE_INTEGER)) {
      return obj.toString()
    }
    return Number(obj)
  }

  if (Array.isArray(obj)) {
    return obj.map(normalizeTypes)
  }

  if (typeof obj === 'object') {
    const result: any = {}
    for (const key of Object.keys(obj)) {
      const value = obj[key]
      if (isIdField(key) && typeof value === 'bigint') {
        result[key] = value.toString()
      } else {
        result[key] = normalizeTypes(value)
      }
    }
    return result
  }

  return obj
}

const CSRF_SKIP_LIST: string[] = import.meta.env.VITE_CSRF_SKIP_LIST?.split(',') || [];
const AUTH_SKIP_LIST: string[] = import.meta.env.VITE_AUTH_SKIP_LIST?.split(',') || [];
const service = axios.create({
    baseURL: import.meta.env.VITE_API_BASE,
    timeout: 5000,
    withCredentials: true,
    transformRequest: [
      (data, headers) => {
        if (typeof data === 'undefined') return data
        if (data instanceof FormData || data instanceof URLSearchParams || data instanceof Blob) return data
        if (typeof data === 'object') {
          headers['Content-Type'] = 'application/json'
          return JSONBigParser.stringify(data)
        }
        return data
      },
    ],
    transformResponse: [
      (data) => {
        if (typeof data !== 'string') return data
        try {
          return normalizeTypes(JSONBigParser.parse(data))
        } catch {
          return data
        }
      },
    ],
}) as ApiRes

// request interceptors
service.interceptors.request.use(
    async config => {
        const isAuthSkip = AUTH_SKIP_LIST.some((path: string) => config.url?.includes(path));
        const isCsrfSkip = CSRF_SKIP_LIST.some((path: string) => config.url?.includes(path));
        const needCSRF = config.meta?.needCSRF !== false && !isCsrfSkip;
        const needAuth = config.meta?.needAuth !== false && !isAuthSkip;

        const token = useAuthStore().accessData.token;
        if (config.method !== 'GET' && needCSRF) {
            let CSRFToken = getCsrfToken()
            if (!CSRFToken) {
                console.log('First request,now try get csrf token');
                try {
                    const response = await fetch(`${import.meta.env.VITE_API_BASE}/auth/csrf-token`, {
                        credentials: 'include'
                    });
                    if (!response.ok) return Promise.reject(new Error(`Failed to fetch CSRF Token.Code ${response.status}`));
                    CSRFToken = getCsrfToken();
                } catch (error) {
                    return Promise.reject(error);
                }
            }
            config.headers['X-XSRF-TOKEN'] = CSRFToken;
        }

        if(needAuth){
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
let isRefreshing = false;
let refreshSubscribers: ((token: string) => void)[] = [];

const onRefreshed = (token: string) => {
    refreshSubscribers.map(cb => cb(token));
};

const addRefreshSubscriber = (cb: (token: string) => void) => {
    refreshSubscribers.push(cb);
};

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
        let showError = error.config.meta?.showError !== false;
        const body = error.response?.data || {};
        let errMessage
        if(error.response) {
            console.log(error.response);
            const status = error.response.status
            errMessage = getErrorMessage(error.response.code)
            if(errMessage === 'UNKNOWN_ERROR') { 
                showError = false ; 
                console.error(error.response.data.message); 
            }
            switch (status) {
                case 401: {
                    const authStore = useAuthStore();
                    const originalRequest = error.config;
                    
                    if (!isRefreshing) {
                        isRefreshing = true;
                        
                        // use fetch to refresh access token to avoid infinit axios looping
                        const csrfTokenForFetch = getCsrfToken();
                        const fetchOptions: RequestInit = {
                            method: 'POST',
                            credentials: 'include',
                            headers: {
                                'Accept': 'application/json, text/plain, */*',
                            }
                        };
                        if (csrfTokenForFetch) {
                            (fetchOptions.headers as Record<string, string>)['X-XSRF-TOKEN'] = csrfTokenForFetch;
                        }

                        fetch(`${import.meta.env.VITE_API_BASE}/auth/refresh`, fetchOptions)
                            .then(res => {
                                if (!res.ok) {
                                  throw new Error("refresh failed");
                                }
                                return res.json();
                            })
                            .then(resData => {
                                const newAccess = resData.data.accessToken;
                                const newExp = resData.data.exp;
                                authStore.setToken(newAccess, newExp);
                                
                                // Auto sync user info and avatar on successful refresh
                                useUserInfoStore().fetchAndUpdateUserInfo().catch(console.error);

                                onRefreshed(newAccess);
                                refreshSubscribers = [];
                            })
                            .catch(err => {
                                // refresh failed
                                console.error('Token refresh failed', err);
                                authStore.removeToken();
                                refreshSubscribers = [];
                                window.location.href = '/login';
                            })
                            .finally(() => {
                                isRefreshing = false;
                            });
                    }
                    
                    // Put original request onto queue and wait for the refresh to complete
                    return new Promise((resolve) => {
                        addRefreshSubscriber((newToken: string) => {
                            originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
                            resolve(service(originalRequest));
                        });
                    });
                }
            }
            if(showError) {
                ElMessage({
                    message: errMessage,
                    type: 'error',
                    duration: 3000
                })
            }
            return Promise.reject(body)
        }else if(error.request) {
            // no response
            errMessage = translate("message.error.networkError")
        }else{
            errMessage = translate("message.error.sendRequestError");
        }
        if(showError) {
            ElMessage({
                message: errMessage,
                type: 'error',
            })
        }
        return Promise.reject(body);
    }
)

// get CSRF Token From cookie
function getCsrfToken() {
    return document.cookie.split(';')
        .map(cookie=> cookie.trim())
        .find(row => row.startsWith("XSRF-TOKEN="))?.split("=")[1];
}

export default service