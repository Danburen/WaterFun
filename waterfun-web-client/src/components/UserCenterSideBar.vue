<template>
  <div class="profile-sidebar">
    <!-- 用户信息区域 -->
    <div class="user-info">
      <div class="avatar-wrapper">
        <el-avatar :size="80" :src="avatar">
          {{ userProfile.nickname ? userProfile.nickname[0] : '用' }}
        </el-avatar>
      </div>
      <div class="user-details">
        <h1 class="nickname">{{ userProfile.nickname || userInfoStore.userInfo.username || '未设置昵称' }}</h1>
      </div>
    </div>

    <!-- 导航菜单 -->
    <ul class="nav-menu">
      <li
        v-for="item in navItems"
        :key="item.id"
        :class="{ 'nav-item': true, 'active': activeTab === item.id }"
        @click="handleNavClick(item)"
      >
        <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
        <span class="nav-text">{{ item.label }}</span>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { ref, shallowRef, watch, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAccountPoolStore } from '~/stores/accountPoolStore';
import { useUserInfoStore } from '~/stores/userInfoStore';
import {
  User,
  Lock,
  Bell,
  Document,
  Headset,
  Switch,
  HomeFilled
// @ts-ignore
} from '@element-plus/icons-vue';


// Props
const props = defineProps({
  userProfile: {
    type: Object,
    default: () => ({
      nickname: '',
      avatar: '',
      joinDate: new Date(),
    })
  },
  activeTab: {
    type: String,
    default: 'profile'
  }
});

const router = useRouter();
const accountPoolStore = useAccountPoolStore();
const userProfileStore = useUserProfileStore();
const userInfoStore = useUserInfoStore();
const currentUid = computed(() => userInfoStore.userInfo?.uid || '')

// Emits
const emit = defineEmits(['tab-change']);

// Default avatar
const avatar = ref('');

// Navigation items
const navItems = shallowRef([
  { id: 'profile', label: '个人资料', icon: User },
  { id: 'security', label: '账号安全', icon: Lock },
  { id: 'accounts', label: '账号管理', icon: Switch },
  { id: 'notification', label: '通知与隐私', icon: Bell },
  { id: 'posts', label: '我的帖子', icon: Document },
  { id: 'personal-space', label: '个人空间', icon: HomeFilled },
  { id: 'customer-service', label: '客服中心', icon: Headset }
]);

// Handle navigation click
const handleNavClick = (item: { id: string }) => {
  if (item.id === 'customer-service') {
    router.push('/customer-service');
  } else if (item.id === 'personal-space') {
    router.push(`/User/${currentUid.value}`);
  } else {
    emit('tab-change', item.id);
  }
};

onMounted(async () => {
  avatar.value = await userProfileStore.getAvatarUrl();
  console.log(avatar.value);
});

watch(() => accountPoolStore.activeUid, async () => {
  avatar.value = await userProfileStore.getAvatarUrl();
});
</script>

<style scoped>
.profile-sidebar {
  width: 260px;
  height: fit-content;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 20px;
  margin-right: 20px;
}

.user-info {
  text-align: center;
  margin-bottom: 24px;
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
  margin-bottom: 16px;
}

.avatar-badge {
  position: absolute;
  bottom: 0;
  right: 0;
  background-color: #fff;
  border-radius: 50%;
  padding: 2px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
}

.user-details {
  margin-bottom: 16px;
}

.nickname {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #333;
}

.nav-menu {
  list-style: none;
  padding: 0;
  margin: 0;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 4px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  color: #666;
}

.nav-item:hover {
  background-color: #f5f7fa;
  color: #333;
}

.nav-item.active {
  background-color: #e6f7ff;
  color: #1890ff;
}

.nav-icon {
  margin-right: 12px;
  font-size: 18px;
}
</style>