<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search,Bell,Message } from '@element-plus/icons-vue'

const router = useRouter()
const searchQuery = ref('')
const unreadNOTICount = ref(5)
const unreadMSGCount = ref(5)
const userName = ref('张三')
const userEmail = ref('zhangsan@example.com')
const userAvatar = ref('https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png')

const handleSearch = () => {
  console.log('搜索内容:', searchQuery.value)
}

const handleNotification = () => {
  console.log('点击了通知按钮')
  unreadNOTICount.value = 0
}

const handleMessage = () => {
  console.log('点击了消息按钮')
  unreadMSGCount.value = 0
}

const handleLogout = () => {
  console.log('用户退出登录')
}

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/settings')
      break
    case 'logout':
      handleLogout()
      break
  }
}
</script>


<template>
  <div class="header-container">
    <el-header class="app-header">
      <!-- 左侧Logo和文字 -->
      <div class="header-left">
        <div class="logo-container">
          <img src="../assets/logo.png" alt="Logo" class="logo" />
          <a class="logo-text menu-item" @click="router.push('/home')">WaterFun</a>
        </div>
        <a class="menu-item" @click="router.push('/community')">
          {{ $t('navbar.community') }}
        </a>
        <a class="menu-item" @click="router.push('/resource')">
          {{ $t('navbar.resource') }}
        </a>
      </div>

      <!-- 中部搜索框和搜索按钮 -->
      <div class="header-center">
        <el-input
            v-model="searchQuery"
            placeholder="请输入搜索内容"
            class="search-input"
            @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button type="primary" :icon="Search"></el-button>
          </template>
        </el-input>
      </div>

      <!-- 右侧个人信息区域 -->
      <div class="header-right">
        <!-- 订阅消息按钮 -->
        <div class="menu-item-small">
          <el-badge
              :hidden="unreadNOTICount === 0"
              :is-dot="unreadNOTICount === 1"
              :value="unreadNOTICount"
              :max="99" class="badge">
            <el-button
                link
                class="reminder-btn"
                @click="handleNotification">
              <el-icon size="22">
                <Bell></Bell>
              </el-icon>
            </el-button>
          </el-badge>
        </div>
        <!-- 消息按钮 -->
        <div class="menu-item-small">
          <el-badge
              :hidden="unreadMSGCount === 0"
              :is-dot="unreadMSGCount === 1"
              :value="unreadMSGCount"
              :max="99" class="badge">
            <el-button
              type="text"
              class="reminder-btn"
              @click="handleMessage">
              <el-icon :size="22">
                <Message></Message>
              </el-icon>
            </el-button>
          </el-badge>
        </div>
        <!-- 个人中心下拉菜单 -->
        <el-dropdown
            trigger="hover"
            placement="bottom"
            @command="handleCommand"
            class="menu-item"
        >
          <div class="user-profile">
            <el-button link class="user-btn">
              <span class="user-name">{{ userName }}</span>
              <el-avatar
                  :size="36"
                  :src="userAvatar"
                  class="user-avatar"
              />
            </el-button>
          </div>

          <!-- 下拉菜单内容 -->
          <template #dropdown>
            <el-dropdown-menu class="user-dropdown">
              <el-dropdown-item class="user-info">
                <div class="user-info-content">
                  <el-avatar
                      :size="38"
                      :src="userAvatar"
                      class="dropdown-avatar"
                  />
                  <div class="user-details">
                    <div class="user-name">{{ userName }}</div>
                    <div class="user-email">{{ userEmail }}</div>
                  </div>
                </div>
              </el-dropdown-item>
              <el-dropdown-item divided command="profile">
                <i class="el-icon-user"></i>个人中心
              </el-dropdown-item>
              <el-dropdown-item command="settings">
                <i class="el-icon-setting"></i>系统设置
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <i class="el-icon-switch-button"></i>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
  </div>
</template>



<style scoped>
.header-container {
  width: 100%;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  position:fixed;
  top: 0;
  z-index: 1000;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 20px;
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
}
.menu-item {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 10px;
  text-decoration: none;
  color: #333;
}

.menu-item-small {
  margin: 0 5px;
}

a:hover {
  color: #66b1ff;
  cursor: pointer;
}

/* 左侧Logo和文字样式 */
.header-left {
  display: flex;
  align-items: center;
}

.logo-container {
  display: flex;
  align-items: center;
}

.logo-container .logo {
  width: 32px;
  height: 32px;
  margin-right: 10px;
}

.logo-container .logo-text {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}
.logo-container .logo-text:hover {
  color: #66b1ff;
  cursor: pointer;
}
/* 中部搜索框样式 */
.header-center {
  flex: 1;
  max-width: 500px;
  margin: 0 20px;
}

.search-input .el-input-group__append {
  background-color: #409EFF;
  color: white;
  border-color: #409EFF;
}

.search-input .el-input-group__append:hover {
  background-color: #66b1ff;
  border-color: #66b1ff;
}

/* 右侧个人信息样式 */
.header-right {
  display: flex;
  align-items: center;
}

.reminder-btn {
  font-size: 18px;
  color: #606266;
}


.badge .el-badge__content {
  top: 10px;
}

/* 用户下拉菜单样式 */
.user-profile .user-btn {
  display: flex;
  align-items: center;
  padding: 20px;
}

.user-profile .user-name {
  margin-right: 8px;
  font-size: 14px;
  color: #606266;
}

.user-profile .user-avatar {
  cursor: pointer;
}

/* 下拉菜单样式 */
.user-dropdown {
  width: 200px;
}

.user-dropdown .user-info {
  padding: 10px 15px;
  cursor: default;
}

.user-dropdown .user-info:hover {
  background-color: white !important;
}

.user-dropdown .user-info-content {
  display: flex;
  align-items: center;
}

.user-dropdown .dropdown-avatar {
  margin-right: 12px;
}

.user-dropdown .user-details .user-name {
  font-weight: bold;
  margin: 0;
}

.user-dropdown .user-details .user-email {
  font-size: 12px;
  color: #909399;
}

/*dropdown item 边距*/
.user-dropdown .el-dropdown-menu__item {
  padding: 10px 15px; /* 增加上下内边距 */
  min-height: 40px;   /* 替代固定height，避免内容挤压 */
  line-height: 1.5;   /* 更灵活的行高 */
}
.user-dropdown .el-dropdown-menu__item i {
  margin-right: 8px;
}
</style>