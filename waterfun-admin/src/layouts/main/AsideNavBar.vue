<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import router, { menuRoutes } from '@/router/index.js'
import IconComponent from "@/components/IconComponent.vue";

const props = defineProps<{
  collapse: boolean;
}>()

const route = useRoute()

const getTopLevelIndex = (topRoute: any) => (topRoute.path || topRoute.name || '') as string

const joinPath = (parentPath: string, childPath: string) => {
  const p = parentPath.endsWith('/') ? parentPath.slice(0, -1) : parentPath
  const c = childPath.startsWith('/') ? childPath.slice(1) : childPath
  return `${p}/${c}`
}

const getStaticPath = (path: string) => path.split('/:')[0]

const activeMenuIndex = computed(() => {
  const currentName = (route.name as string | undefined) ?? ''
  const currentPath = route.path

  for (const topRoute of menuRoutes as any[]) {
    if (!topRoute.children || topRoute.children.length === 0) {
      if (topRoute.name === currentName || topRoute.path === currentPath) {
        return (topRoute.name || topRoute.path || '') as string
      }
      continue
    }

    for (const child of topRoute.children) {
      if (child.meta?.isDetail) {
        continue
      }

      const fullPath = getStaticPath(joinPath(topRoute.path, child.path))
      if (child.name === currentName || currentPath.startsWith(fullPath)) {
        return child.name as string
      }
    }
  }

  return ''
})

const openedTopMenus = computed(() => {
  const currentPath = route.path
  const top = (menuRoutes as any[]).find(item => {
    if (!item.children || item.children.length === 0) {
      return false
    }
    return currentPath === item.path || currentPath.startsWith(`${item.path}/`)
  })

  return top ? [getTopLevelIndex(top)] : []
})

const handleMenuClick = (routeName:string) => {
  router.push({ name: routeName })
}
</script>

<template>
  <el-menu
    class="aside-menu"
    :default-active="activeMenuIndex"
    :default-openeds="openedTopMenus"
    :unique-opened="true"
    :collapse="collapse"
  >
    <template v-for="route in menuRoutes">
      <el-sub-menu v-if="route.children && route.children.length" :index="getTopLevelIndex(route)">
        <template #title>
          <el-icon class="menu-icon"><IconComponent :icon="route.meta.icon" /></el-icon>
          <span>{{ $t(`nav.${route.meta.locale}`) }}</span>
        </template>
        <el-menu-item @click="handleMenuClick(child.name as string)"  
          v-for="child in route.children.filter(child => !child.meta.isDetail)" 
          :index="child.name as string"
        >
          <span>{{ $t(`nav.${child.meta.locale}`)}}</span>
        </el-menu-item>
      </el-sub-menu>
      <el-menu-item 
        @click="handleMenuClick(route.name as string)" 
        v-else-if="! route.meta.isDetail" 
        :index="route.name as string"
      >
        <el-icon class="menu-icon"><IconComponent :icon="route.meta.icon" /></el-icon>
        <span>{{ $t(`nav.${route.meta.locale}`)}}</span>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<style scoped>
.menu-icon{
  width: 1em;
  height: 1em;
  margin-right: 8px
}

.aside-menu {
  min-height: 100%;
  width: 175px;
}

a {
  text-decoration: none;
}
</style>