<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
//@ts-ignore
import { Bell, Message, Search } from '@element-plus/icons-vue'
import { useAuth } from "~/composables/useAuth"
import { useUserInfoStore } from "~/stores/userInfoStore"
import logoSrc from '~/assets/logo.svg'
import { useUserProfileStore } from "~/stores/userProfileStore"
import { useNotificationStore } from "~/stores/notificationStore"

const { isLoggedIn, logout } = useAuth()
const userInfoStore = useUserInfoStore()
const userProfileStore = useUserProfileStore()
const notificationStore = useNotificationStore()
const router = useRouter()
const route = useRoute()

const searchQuery = ref('')
const userAvatar = ref('https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png')

const userName = computed(() => userInfoStore.userInfo.username || '未登录')

const navItems = [
  { key: 'home', path: '/' },
  { key: 'post', path: '/post' },
  { key: 'about', path: '/about' },
]

const isActive = (path: string) => {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

const handleSearch = () => {
  if (searchQuery.value.trim()) {
    router.push({ path: '/post', query: { q: searchQuery.value.trim() } })
  }
}

const handleLogout = () => {
  logout()
  router.push('/login')
}

const handleCommand = (command: string) => {
  switch (command) {
    case 'profile': router.push('/profile'); break
    case 'account': router.push('/profile/account'); break
    case 'logout': handleLogout(); break
  }
}

const getAvatarUrl = async () => {
  try {
    return await userProfileStore.getAvatarUrl() || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
  } catch {
    return 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
  }
}

onMounted(async () => {
  userAvatar.value = await getAvatarUrl()
  if (isLoggedIn.value) {
    notificationStore.fetchUnreadCount()
  }
})
</script>
<template>
  <header class="header">
    <div class="header-inner">
      <NuxtLink to="/" class="logo">
        <img :src="logoSrc" alt="WaterFun" class="logo-img">
        <span class="logo-text">WaterFun</span>
      </NuxtLink>
      <nav class="nav">
        <NuxtLink v-for="item in navItems" :key="item.key" :to="item.path" :class="['nav-link', { active: isActive(item.path) }]">
          {{ $t(`navbar.${item.key}`) }}
        </NuxtLink>
      </nav>
      <div class="header-spacer" />
      <div class="search-box">
        <input v-model="searchQuery" type="text" :placeholder="$t('option.search')" @keyup.enter="handleSearch">
        <button @click="handleSearch"><el-icon><Search /></el-icon></button>
      </div>
      <div class="header-actions">
        <ClientOnly>
          <template v-if="isLoggedIn">
            <button class="icon-btn" @click="router.push('/message-center')">
              <el-badge :hidden="notificationStore.unreadCount === 0" :value="notificationStore.unreadCount" :max="99">
                <el-icon size="20"><Bell /></el-icon>
              </el-badge>
            </button>
            <button class="icon-btn" @click="router.push('/message-center')">
              <el-icon size="20"><Message /></el-icon>
            </button>
            <el-dropdown trigger="hover" placement="bottom" @command="handleCommand">
              <div class="user-menu">
                <img :src="userAvatar" alt="avatar" class="user-avatar">
                <span class="user-name">{{ userName }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="account">账号设置</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <button class="login-btn" @click="router.push('/login')">登录</button>
            <button class="register-btn" @click="router.push('/register')">注册</button>
          </template>
          <template #fallback>
            <div class="header-actions-skeleton" />
          </template>
        </ClientOnly>
      </div>
    </div>
  </header>
</template>
<style scoped>
.header {
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 24px;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 20px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  flex-shrink: 0;
}

.logo-img {
  width: 32px;
  height: 36px;
  object-fit: contain;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.5px;
}

.nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.nav-link {
  padding: 8px 14px;
  color: #64748b;
  text-decoration: none;
  font-size: 15px;
  font-weight: 500;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.nav-link:hover {
  color: #3b82f6;
  background: #eff6ff;
}

.nav-link.active {
  color: #3b82f6;
  background: #eff6ff;
}

.header-spacer {
  flex: 1;
}

.search-box {
  position: relative;
  width: 320px;
  flex-shrink: 0;
}

.search-box input {
  width: 100%;
  padding: 10px 42px 10px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  font-size: 14px;
  background: #f8fafc;
  color: #1e293b;
  outline: none;
  box-sizing: border-box;
  transition: all 0.2s ease;
}

.search-box input:focus {
  border-color: #3b82f6;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.search-box button {
  position: absolute;
  right: 4px;
  top: 50%;
  transform: translateY(-50%);
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.search-box button:hover {
  color: #3b82f6;
  background: #eff6ff;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.icon-btn {
  width: 38px;
  height: 38px;
  border: none;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: all 0.2s ease;
}

.icon-btn:hover {
  color: #3b82f6;
  background: #eff6ff;
}


.user-menu {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 10px 4px 4px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-left: 4px;
}

.user-menu:hover {
  background: #f1f5f9;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #e2e8f0;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

.login-btn, .register-btn {
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.login-btn {
  background: transparent;
  color: #64748b;
}

.login-btn:hover {
  color: #3b82f6;
  background: #eff6ff;
}

.register-btn {
  background: #3b82f6;
  color: white;
}

.register-btn:hover {
  background: #2563eb;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.header-actions-skeleton {
  width: 120px;
  height: 36px;
}

@media (max-width: 1024px) {
  .search-box {
    width: 200px;
  }
}

@media (max-width: 768px) {
  .header-inner {
    padding: 0 16px;
    gap: 12px;
  }
  .nav {
    display: none;
  }
  .search-box {
    width: 160px;
  }
  .user-name {
    display: none;
  }
}
</style>
