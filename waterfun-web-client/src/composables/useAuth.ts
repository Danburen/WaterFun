import { computed } from "vue"
import { useRouter } from "vue-router"
import { useUserInfoStore } from "~/stores/userInfoStore"
import { useUserProfileStore } from "~/stores/userProfileStore"
import { useAuthStore } from "~/stores/authStore"
import { useUserAccountStore } from "~/stores/userAccountStore"
import { useAccountPoolStore } from "~/stores/accountPoolStore"
import type { PoolAccount } from "~/stores/accountPoolStore"
import {
    login,
    register,
    logout,
 } from "~/api/authApi"
import {ElMessage} from "element-plus"
import { translate } from "~/utils/translator"
import type { LoginRequest, RegisterRequest } from "~/api/authApi"

import { generateFingerprint } from "@waterfun/web-core/src/fingerprint"
import type { AccessTokenResponse } from "@waterfun/web-core/src/types/api/auth"

function buildPoolAccount(): PoolAccount {
    const authStore = useAuthStore()
    const userInfoStore = useUserInfoStore()
    const u = userInfoStore.userInfo
    const expireAt = u.avatar?.expireAt
    return {
        uid: u.uid,
        username: u.username,
        nickname: u.nickname,
        avatarUrl: u.avatar?.url || '',
        avatarExpireAt: expireAt,
        token: authStore.accessData.token,
        expire: authStore.accessData.expire,
    }
}

export const useAuth = () => {
    const authStore = useAuthStore()
    const userInfoStore = useUserInfoStore()
    const userProfileStore = useUserProfileStore()
    const userAccountStore = useUserAccountStore()
    const accountPoolStore = useAccountPoolStore()

    const router = useRouter()

    const handleAuthSuccess = async (loginRes: AccessTokenResponse) => {
        authStore.setToken(loginRes.accessToken,
            Date.now() + loginRes.exp * 1000)

        await userInfoStore.fetchAndUpdateUserInfo()
        await userProfileStore.fetchAndUpdateUserProfile()
        await userAccountStore.fetchAccountInfoAndUpdate()

        accountPoolStore.saveCurrentAccount(buildPoolAccount())

        authStore.fromPool = false
        authStore.lastBrowserLoginUid = userInfoStore.userInfo.uid

        const { useNotificationStore } = await import('~/stores/notificationStore')
        useNotificationStore().connectSSE()
    }

    const tryLogin = async (loginRequest: LoginRequest, type: string) => {
        const loginRes = await login(loginRequest, type)
        return handleAuthSuccess(loginRes.data).then(() => {
            ElMessage({
                message: translate("message.success.loginSuccess"),
                type: "success",
            })
        }).catch(err => {
            return Promise.reject(err)
        })
    }

    const tryRegister = async (registerRequest: RegisterRequest) => {
        const loginRes = await register(registerRequest)
        return handleAuthSuccess(loginRes.data).then(() => {
            ElMessage({
                message: translate('message.success.registerSuccess'),
                type: "success",
            })
            router.push("/")
        }).catch(err => {
            return Promise.reject(err)
        })
    }

    const tryLogout = async () => {
        const dfp = await generateFingerprint()
        const currentUid = userInfoStore.userInfo.uid
        return logout(dfp).finally(() => {
            userInfoStore.clearUserInfo()
            userProfileStore.clearUserProfile()
            authStore.removeToken()
            accountPoolStore.removeAccount(currentUid)
            import('~/stores/notificationStore').then(({ useNotificationStore }) => {
                useNotificationStore().disconnectSSE()
            })
        })
    }

    const switchAccount = async (uid: string): Promise<boolean> => {
        const target = accountPoolStore.accounts.find(a => a.uid === uid)
        if (!target) return false

        if (Date.now() >= target.expire) {
            ElMessage.warning('该账号登录已过期，请重新登录')
            setTimeout(() => router.push('/login'), 1500)
            return false
        }

        accountPoolStore.saveCurrentAccount(buildPoolAccount())
        accountPoolStore.activeUid = uid

        authStore.fromPool = true
        authStore.setToken(target.token, target.expire)
        userInfoStore.updateUserInfo({
            uid: target.uid,
            username: target.username,
            nickname: target.nickname,
            avatar: {
                url: target.avatarUrl,
                expireAt: String(target.avatarExpireAt),
            },
        })
        if (target.avatarUrl) {
            const expireTime = typeof target.avatarExpireAt === 'string'
                ? new Date(target.avatarExpireAt).getTime() : (target.avatarExpireAt as number) || 0
            userProfileStore.updateAvatar(target.avatarUrl, expireTime, target.uid)
        }

        import('~/stores/notificationStore').then(({ useNotificationStore }) => {
            useNotificationStore().disconnectSSE()
            useNotificationStore().connectSSE()
        })

        userInfoStore.fetchAndUpdateUserInfo().catch(() => {})
        userProfileStore.fetchAndUpdateUserProfile().catch(() => {})
        userAccountStore.fetchAccountInfoAndUpdate().catch(() => {})

        ElMessage.success('已切换账号')

        return true
    }

    const isLoggedIn = computed(() => {
        const expireIn = Number(authStore.accessData.expire)
        return userInfoStore.userInfo.uid !== '' && (Date.now() < expireIn)
    })

    return { tryLogin, tryRegister, logout: tryLogout, isLoggedIn, switchAccount }
}
