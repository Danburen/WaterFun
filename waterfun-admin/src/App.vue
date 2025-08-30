<script setup lang="ts">
import {
  Edit,House, VideoCamera , ArrowRight
} from '@element-plus/icons-vue'
import MainNavBar from "@/components/MainNavBar.vue";
import { ref, onMounted } from 'vue'
import type {TagItem} from "@/types/TagItemsType";
import TagNavigation from "@/components/TagNavigation.vue";

const menuCollapse = ref(false)
const tagNavContainer = ref(null)

const handleWheel = (e) => {
  if(! tagNavContainer.value) return
  const scroll = tagNavContainer.value
  console.log(scroll.scrollLeft,e.deltaY)
  scroll.scrollLeft = scroll.scrollLeft + e.deltaY
}

const tagList = ref<TagItem[]>([
  {
    name: 'HomePage',
    locale: 'nav.home',
    to: '/',
    closeable: false
  },
  {
    name: 'PermissionManager',
    locale: 'nav.permissionManager',
    to: '/permissionManager',
    closeable: true
  },
  {
    name: 'OnlineUser',
    locale: 'nav.onlineUser',
    to: '/onlineUser',
    closeable: true
  },
  {
    name: 'OnlineUser',
    locale: 'nav.onlineUser',
    to: '/onlineUser',
    closeable: true
  },
  {
    name: 'OnlineUser',
    locale: 'nav.onlineUser',
    to: '/onlineUser',
    closeable: true
  },
  {
    name: 'OnlineUser',
    locale: 'nav.onlineUser',
    to: '/onlineUser',
    closeable: true
  },
  {
    name: 'OnlineUser',
    locale: 'nav.onlineUser',
    to: '/onlineUser',
    closeable: true
  },
  {
    name: 'OnlineUser',
    locale: 'nav.onlineUser',
    to: '/onlineUser',
    closeable: true
  },
  {
    name: 'OnlineUser',
    locale: 'nav.onlineUser',
    to: '/onlineUser',
    closeable: true
  },
  {
    name: 'OnlineUser',
    locale: 'nav.onlineUser',
    to: '/onlineUser',
    closeable: true
  },
  {
    name: 'OnlineUser',
    locale: 'nav.onlineUser',
    to: '/onlineUser',
    closeable: true
  },
  {
    name: 'OnlineUser',
    locale: 'nav.onlineUser',
    to: '/onlineUser',
    closeable: true
  },
  {
    name: 'OnlineUser',
    locale: 'nav.onlineUser',
    to: '/onlineUser',
    closeable: true
  },
  {
    name: 'OnlineUser',
    locale: 'nav.onlineUser',
    to: '/onlineUser',
    closeable: true
  },
  {
    name: 'OnlineUser',
    locale: 'nav.onlineUser',
    to: '/onlineUser',
    closeable: true
  },
  {
    name: 'OnlineUser',
    locale: 'nav.onlineUser',
    to: '/onlineUser',
    closeable: true
  },
  {
    name: 'OnlineUser',
    locale: 'nav.onlineUser',
    to: '/onlineUser',
    closeable: true
  },
  {
    name: 'OnlineUser',
    locale: 'nav.onlineUser',
    to: '/onlineUser',
    closeable: true
  }
])

onMounted(() => {

});

</script>

<template>
  <div class="container full-page-container">
    <!-- Global Header -->
    <MainNavBar @collapse="menuCollapse = !menuCollapse" />
    <!-- Main Container-->
    <div class="main-container">
        <el-menu class="aside-menu" default-active="1" :collapse="menuCollapse" >
          <el-menu-item index="1">
            <el-icon><House /></el-icon>
            <template #title>
              <span>{{ $t('nav.home') }}</span>
            </template>
          </el-menu-item>
          <el-sub-menu index="2">
            <template #title>
              <el-icon class="menu-icon"><Edit /></el-icon>
              <span>{{ $t('nav.manager') }}</span>
            </template>
            <el-menu-item><span>{{ $t('nav.userManager') }}</span></el-menu-item>
            <el-menu-item><span>{{ $t('nav.roleManager') }}</span></el-menu-item>
            <el-menu-item><span>{{ $t('nav.permissionManager') }}</span></el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="3">
            <template #title>
              <el-icon class="menu-icon"><VideoCamera /></el-icon>
              <span>{{ $t('nav.statusMonitor') }}</span>
            </template>
            <el-menu-item><span>{{ $t('nav.serverMonitor') }}</span></el-menu-item>
            <el-menu-item><span>{{ $t('nav.dynamicWorld') }}</span></el-menu-item>
            <el-menu-item><span>{{ $t('nav.onlineUser') }}</span></el-menu-item>
          </el-sub-menu>
        </el-menu>
      <div class="content-container">
        <div class="content-header default-border-bottom"> <TagNavigation v-model="tagList" /></div>
        <div class="content-main">Main body</div>
        <div class="content-footer default-border-top">
          <div class="copyright">CopyRight © WaterFun 2025</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  min-height: 100vh
}
.main-container {
  display: flex;
  flex-grow: 1;
  min-height: 0;
  min-width: 0;
}

.content-container {
  background: white;
  display: flex;
  flex: 1;
  min-height: 0;
  min-width: 0;
  flex-direction: column;
}

.content-main {
  flex: 1; /* 占据剩余空间 */
  padding: 20px;
  overflow-y: auto; /* 内容过多时滚动 */
}

.content-footer {
  max-height: 30px;
  font-size: 12px;
}

.copyright {
  float: right;
  padding: 0.5em 1em;
}

.menu-icon{
  width: 1em;
  height: 1em;
  margin-right: 8px
}

.aside-menu {
  min-height: 100%;
}

.content-header {
  padding: 0 1em;
}

</style>
