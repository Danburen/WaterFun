<template>
  <div class="profile-page">
    <HeaderNavMenu />
    <!-- 主要内容区域 -->
    <div class="profile-layout">
      <!-- 左侧侧边栏 -->
      <UserCenterSideBar 
        :user-profile="userProfileData" 
        :active-tab="activeTab" 
        @tab-change="handleTabChange" 
      />
      
      <!-- 右侧内容区域 -->
      <div class="profile-content">
        <NuxtPage />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({
  ssr: false
})
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import UserCenterSideBar from '~/components/UserCenterSideBar.vue';
import { useUserInfoStore } from "~/stores/userInfoStore";
import { useUserProfileStore } from "~/stores/userProfileStore";
import { ElMessage } from 'element-plus';

const router = useRouter();
const route = useRoute();
const userInfoStore = useUserInfoStore();
const userProfileStore = useUserProfileStore();

const userInfo = computed(() => userInfoStore.userInfo);
const userProfile = computed(() => userProfileStore.userProfile);

const userProfileData = ref({
  nickname: userInfo.value.username,
  avatar: '',
  joinDate: userInfo.value.createdAt
});

onMounted(async () => {
  await userProfileStore.fetchAndUpdateUserProfile().catch(console.error);

  try {
    userProfileData.value.avatar = await userProfileStore.getAvatarUrl();
  } catch (error) {
    console.error('Failed to load avatar:', error);
  }
  userProfileData.value.nickname = userInfo.value.username;
  userProfileData.value.joinDate = userInfo.value.createdAt;
  
  if (route.path === '/profile') {
    router.push('/profile/info');
  }
});

const activeTab = computed(() => {
  const path = route.path;
  if (path.includes('/profile/info')) return 'profile';
  if (path.includes('/profile/account')) return 'security';
  if (path.includes('/profile/notifications')) return 'notification';
  return 'profile';
});


// 处理标签切换
const handleTabChange = (tabId: string) => {
  const map: Record<string, string> = {
    profile: '/profile/info',
    security: '/profile/account',
    notification: '/profile/notifications'
  }
  router.push(map[tabId] || '/profile/info');
  console.log('切换到标签:', tabId, map[tabId], router.currentRoute.value.path);
};



</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.profile-layout {
  display: flex;
  flex: 1;
  padding: 30px 100px;
  background-color: #f5f7fa;
}

.profile-content {
  flex: 1;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
  overflow: hidden;
  margin: 0 20px;
  padding: 30px;
  min-height: 500px;
}

.content-header {
  padding: 20px 30px;
  border-bottom: 1px solid #e8e8e8;
}

.content-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0;
}
</style>