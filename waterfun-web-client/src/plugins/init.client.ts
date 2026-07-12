import { defineNuxtPlugin } from 'nuxt/app';
import { useAuthStore } from '~/stores/authStore';
import { useUserInfoStore } from '~/stores/userInfoStore';
import { useUserAccountStore } from '~/stores/userAccountStore';
import { useAccountPoolStore } from '~/stores/accountPoolStore';
import { useUserProfileStore } from '~/stores/userProfileStore';

export default defineNuxtPlugin(async (nuxtApp) => {
    nuxtApp.hook('app:mounted', () => {
        const authStore = useAuthStore();
        const poolStore = useAccountPoolStore();
        const userInfoStore = useUserInfoStore();

        if (poolStore.activeUid && poolStore.isTokenValid(poolStore.activeUid)) {
            const active = poolStore.activeAccount
            if (active && (!authStore.isAccess || authStore.accessData.token !== active.token)) {
                authStore.fromPool = true;
                authStore.setToken(active.token, active.expire);
                userInfoStore.updateUserInfo({
                    uid: active.uid,
                    username: active.username,
                    nickname: active.nickname,
                    avatar: { url: active.avatarUrl, expireAt: String(active.avatarExpireAt) },
                });
                if (active.avatarUrl) {
                    const expireTime = typeof active.avatarExpireAt === 'string'
                        ? new Date(active.avatarExpireAt).getTime() : (active.avatarExpireAt as number) || 0
                    useUserProfileStore().updateAvatar(active.avatarUrl, expireTime, active.uid)
                }
            }
        }

        if (authStore.isAccess) {
            const userAccountStore = useUserAccountStore();

            userInfoStore.fetchAndUpdateUserInfo().catch(console.error);
            userAccountStore.fetchAccountInfoAndUpdate().catch(console.error);

            import('~/stores/notificationStore').then(({ useNotificationStore }) => {
                useNotificationStore().connectSSE();
            });
        }
    });
});
