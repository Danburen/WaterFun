import axios from 'axios'
import { ElMessage } from "element-plus";
import { translate } from "~/utils/translator";
import {useAuthStore} from "~/stores/authStore";
import {useUserInfoStore} from "~/stores/userInfoStore";
import {useAccountPoolStore} from "~/stores/accountPoolStore";
import { generateFingerprint } from "@waterfun/web-core/src/fingerprint";
import JSONBig from 'json-bigint'

declare module 'axios' {
    interface AxiosRequestConfig {
        meta?: {
            needAuth?: boolean;
        };
    }
}

const JSONBigParser = JSONBig({ useNativeBigInt: true, alwaysParseAsBig: true })

function isIdField(key: string): boolean {
  return /^(id|uid|postId|parentId|rootId|commentId|categoryId|tagId|authorId|bizId)$/i.test(key)
}

function normalizeResponseTypes(obj: any): any {
  if (obj === null || obj === undefined) return obj

  if (typeof obj === 'bigint') {
    if (obj > BigInt(Number.MAX_SAFE_INTEGER) || obj < BigInt(Number.MIN_SAFE_INTEGER)) {
      return obj.toString()
    }
    return Number(obj)
  }

  if (Array.isArray(obj)) {
    return obj.map(normalizeResponseTypes)
  }

  if (typeof obj === 'object') {
    const result: any = {}
    for (const key of Object.keys(obj)) {
      const value = obj[key]
      if (isIdField(key) && typeof value === 'bigint') {
        result[key] = value.toString()
      } else {
        result[key] = normalizeResponseTypes(value)
      }
    }
    return result
  }

  return obj
}

function normalizeRequestTypes(obj: any): any {
  if (obj === null || obj === undefined) return obj

  if (Array.isArray(obj)) {
    return obj.map((item) => {
      if (typeof item === 'string' && /^\d{1,19}$/.test(item)) {
        return BigInt(item)
      }
      return normalizeRequestTypes(item)
    })
  }

  if (typeof obj === 'object') {
    const result: any = {}
    for (const key of Object.keys(obj)) {
      const value = obj[key]
      if (isIdField(key) && typeof value === 'string' && /^\d{1,19}$/.test(value)) {
        result[key] = BigInt(value)
      } else {
        result[key] = normalizeRequestTypes(value)
      }
    }
    return result
  }

  return obj
}

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
          return JSONBigParser.stringify(normalizeRequestTypes(data))
        }
        return data
      },
    ],
    transformResponse: [
      (data) => {
        if (typeof data !== 'string') return data
        try {
          return normalizeResponseTypes(JSONBigParser.parse(data))
        } catch {
          return data
        }
      },
    ],
})

/** 专用于 token 刷新的 axios 实例（无 response 拦截器，避免循环） */
const refreshService = axios.create({
    baseURL: import.meta.env.VITE_API_BASE,
    timeout: 5000,
    withCredentials: true,
    transformResponse: [
      (data) => {
        if (typeof data !== 'string') return data
        try {
          return normalizeResponseTypes(JSONBigParser.parse(data))
        } catch {
          return data
        }
      },
    ],
})

// request interceptors
service.interceptors.request.use(
    async (config: import('axios').InternalAxiosRequestConfig) => {
        const isAuthSkip = AUTH_SKIP_LIST.some((path: string) => config.url?.includes(path));
        const needAuth = config.meta?.needAuth !== false && !isAuthSkip;

        const token = useAuthStore().accessData.token;
        if(needAuth && token){
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
    refreshSubscribers.splice(0).forEach(cb => cb(token));
};

const addRefreshSubscriber = (cb: (token: string) => void) => {
    refreshSubscribers.push(cb);
};

service.interceptors.response.use(
    (response: import('axios').AxiosResponse) => {
        if (response.status !== 200) {
            console.error(response);
            return Promise.reject(new Error(response.data.message || 'Error'))
        } else {
            return response.data
        }
    },
    error => {
        const body = error.response?.data || {};
        if(error.response) {
            console.log(error.response);
            const status = error.response.status
            switch (status) {
                case 401: {
                    const authStore = useAuthStore();
                    const originalRequest = error.config;

                    const authPaths = ['/login', '/register', '/auth/']
                    const onAuthPage = authPaths.some(p => window.location.pathname.includes(p))
                    if (onAuthPage) {
                        ElMessage({
                            message: body.message && body.message !== 'error' ? body.message : (body.code || translate('message.error.unknownError')),
                            type: 'error',
                            duration: 3000
                        })
                        return Promise.reject(body)
                    }

                    function showMessageAndRedirect() {
                        const hasToken = !!authStore.accessData.token
                        authStore.removeToken()
                        const msg = hasToken
                            ? '登录已过期，请重新登录'
                            : '该功能需要登录才能使用，正在跳转到登录界面'
                        ElMessage.warning(msg)
                        setTimeout(() => {
                            if (!window.location.pathname.includes('/login')) {
                                window.location.href = '/login'
                            }
                        }, 1500)
                    }

                    if (!isRefreshing) {
                        isRefreshing = true;

                        if (!authStore.accessData.token) {
                            showMessageAndRedirect()
                            isRefreshing = false
                            return Promise.reject(body)
                        }

                        // 不检查 isAccess —— token 过期正是要 refresh 的场景
                        // 如果 RT 也过期了，refresh 端点返回非 200，catch 里会跳登录

                        if (authStore.fromPool && authStore.lastBrowserLoginUid && authStore.lastBrowserLoginUid !== useUserInfoStore().userInfo.uid) {
                            onRefreshed('')
                            showMessageAndRedirect()
                            isRefreshing = false
                            return Promise.reject(body)
                        }

                        (async () => {
                            try {
                                const deviceFp = await generateFingerprint();
                                const res = await refreshService.post('/auth/refresh', null, {
                                    params: { deviceFp },
                                });
                                const data = res.data as any;
                                const newAccess = data.data.accessToken;
                                const newExp = Date.now() + data.data.exp * 1000;
                                authStore.setToken(newAccess, newExp);

                                useUserInfoStore().fetchAndUpdateUserInfo().catch(console.error);
                                const poolStore = useAccountPoolStore();
                                if (poolStore.activeUid) {
                                    poolStore.updateToken(poolStore.activeUid, newAccess, newExp);
                                }

                                onRefreshed(newAccess);
                            } catch (err) {
                                console.error('Token refresh failed', err);
                                onRefreshed('')
                                showMessageAndRedirect()
                            } finally {
                                isRefreshing = false;
                            }
                        })();
                    }

                    return new Promise((resolve, reject) => {
                        addRefreshSubscriber((newToken: string) => {
                            if (!newToken) {
                                reject(body)
                                return
                            }
                            originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
                            resolve(service(originalRequest));
                        });
                    });
                }
            }
            ElMessage({
                message: body.message && body.message !== 'error' ? body.message : (body.code || translate('message.error.unknownError')),
                type: 'error',
                duration: 3000
            })
            return Promise.reject(body)
        }else if(error.request) {
            ElMessage({
                message: translate("message.error.networkError"),
                type: 'error',
            })
        }else{
            ElMessage({
                message: translate("message.error.sendRequestError"),
                type: 'error',
            })
        }
        return Promise.reject(body);
    }
)

export default service