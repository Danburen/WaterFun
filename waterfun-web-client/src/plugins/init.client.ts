import { defineNuxtPlugin } from '#app';
import { useAuthStore } from '~/stores/authStore';
import { useUserInfoStore } from '~/stores/userInfoStore';
import { useUserAccountStore } from '~/stores/userAccountStore';

export default defineNuxtPlugin(async (nuxtApp) => {
    nuxtApp.hook('app:mounted', () => {
        const authStore = useAuthStore();
        // 每次应用启动时，主动再更新一次用户的基础资料（如用户数据、头像URL及过期时间）
        if (authStore.isAccess) {
            const userInfoStore = useUserInfoStore();
            const userAccountStore = useUserAccountStore();
            
            userInfoStore.fetchAndUpdateUserInfo().catch(console.error);
            userAccountStore.fetchAccountInfoAndUpdate().catch(console.error);

            // 全局 SSE 实时通知连接
            import('~/stores/notificationStore').then(({ useNotificationStore }) => {
                useNotificationStore().connectSSE();
            });
        }
    });
});
