<template>
  <div class="profile-sidebar">
    <!-- 用户信息区域 -->
    <div class="user-info">
      <div class="avatar-wrapper">
        <el-avatar :size="80" :src="avatar || '/waterfun-web-client/icons/default-avatar.png'">
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
        :class="{ 'nav-item': true, 'active': activeTab === item.id, 'disabled': item.disabled }"
        @click="handleNavClick(item)"
      >
        <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
        <span class="nav-text">{{ item.label }}</span>
        <el-badge v-if="item.badge" :value="item.badge" class="nav-badge" />
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { ref, shallowRef } from 'vue';
import { formatDate } from '~/utils/date';
import {
  User,
  Lock,
  Bell,
  Document,
  Check,
  Star,
  DataAnalysis,
  Setting,
  Coin,
  Ticket,
  ShoppingCart
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

const userProfileStore = useUserProfileStore();
const userInfoStore = useUserInfoStore();

// Emits
const emit = defineEmits(['tab-change']);

// Default avatar
const avatar = ref('');

// Navigation items
const navItems = shallowRef([
  { id: 'profile', label: '个人资料', icon: User, disabled: false },
  { id: 'security', label: '账号安全', icon: Lock, disabled: false },
  { id: 'notification', label: '通知与隐私', icon: Bell, disabled: false },
  { id: 'notes', label: '个人笔记', icon: Document, disabled: true },
  { id: 'solutions', label: '我的解题', icon: Check, disabled: true },
  { id: 'creator', label: '创作激励', icon: Star, disabled: true, badge: 'Beta' },
  { id: 'analysis', label: '进展分析', icon: DataAnalysis, disabled: true },
  { id: 'settings', label: '进度管理', icon: Setting, disabled: true },
  { id: 'points', label: '积分仓', icon: Coin, disabled: true },
  { id: 'coupons', label: '优惠券', icon: Ticket, disabled: true },
  { id: 'orders', label: '订单', icon: ShoppingCart, disabled: true }
]);

// Handle navigation click
const handleNavClick = (item: { id: string; disabled: boolean }) => {
  if (!item.disabled) {
    emit('tab-change', item.id);
  }
};

onMounted(async () => {
  avatar.value = await userProfileStore.getAvatarUrl();
  console.log(avatar.value);
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

.nav-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.nav-item.disabled:hover {
  background-color: transparent;
  color: #666;
}

.nav-icon {
  margin-right: 12px;
  font-size: 18px;
}

.nav-badge {
  margin-left: auto;
}
</style>