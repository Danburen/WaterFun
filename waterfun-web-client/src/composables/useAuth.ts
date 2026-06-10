import { computed } from "vue";
import { useRouter } from "vue-router";
import { useUserInfoStore } from "~/stores/userInfoStore";
import { useUserProfileStore } from "~/stores/userProfileStore";
import { useAuthStore } from "~/stores/authStore";
import { 
    login,
    register,
    logout,
 } from "~/api/authApi";
import {ElMessage} from "element-plus";
import { translate } from "~/utils/translator";
import type { LoginRequest, RegisterRequest } from "~/api/authApi";

import { generateFingerprint } from "@waterfun/web-core/src/fingerprint";
import type { AccessTokenResponse } from "@waterfun/web-core/src/types/api/auth";


export const useAuth = () => {
    const authStore = useAuthStore();
    const userInfoStore = useUserInfoStore();
    const userProfileStore = useUserProfileStore();
    const userAccountStore = useUserAccountStore();
    
    const router = useRouter();

    const handleAuthSuccess = async  (loginRes: AccessTokenResponse) => {
        authStore.setToken(loginRes.accessToken,
            Date.now() + loginRes.exp * 1000)

        await userInfoStore.fetchAndUpdateUserInfo();
        await userProfileStore.fetchAndUpdateUserProfile();
        await userAccountStore.fetchAccountInfoAndUpdate();
        console.log('用户账户信息:', userAccountStore.userAccount);

        const { useNotificationStore } = await import('~/stores/notificationStore');
        useNotificationStore().connectSSE();
    }


    const tryLogin = async (loginRequest:LoginRequest, type: string) => {
        const loginRes = await login(loginRequest, type);
        return handleAuthSuccess(loginRes.data).then(()=>{
            ElMessage({
                message: translate("message.success.loginSuccess"),
                type: "success",
            })
        }).catch(err=>{
            return Promise.reject(err);
        });
    }

    const tryRegister =  async (registerRequest: RegisterRequest) => {
        const loginRes = await register(registerRequest)
        return handleAuthSuccess(loginRes.data).then(()=>{
            ElMessage({
                message: translate('message.success.registerSuccess'),
                type: "success",
            });
            router.push("/");
        }).catch(err=>{
            return Promise.reject(err);
        })
    }

    const tryLogout = async () => {
        const dfp = await generateFingerprint()
        return logout(dfp).then(() => {
            userInfoStore.clearUserInfo();
            userProfileStore.clearUserProfile();
            authStore.removeToken();
            import('~/stores/notificationStore').then(({ useNotificationStore }) => {
                useNotificationStore().disconnectSSE();
            });
        })
    }

    const isLoggedIn = computed(()=>{
        const expireIn = Number(authStore.accessData.expire);
        console.log(userInfoStore.userInfo.uid, expireIn)
        return userInfoStore.userInfo.uid !== null && (Date.now() < expireIn);
    })

    return { tryLogin, tryRegister, logout: tryLogout, isLoggedIn }
}