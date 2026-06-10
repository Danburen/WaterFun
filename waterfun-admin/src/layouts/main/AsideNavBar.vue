<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import router, { menuRoutes } from '@/router'

const props = defineProps<{
  collapse: boolean;
}>()

const route = useRoute()

const expandedSections = ref<Set<string>>(new Set())

const initExpanded = () => {
  const set = new Set<string>()
  for (const topRoute of menuRoutes as any[]) {
    if (topRoute.children && topRoute.children.length > 0) {
      const path = topRoute.path || topRoute.name || ''
      set.add(path)
    }
  }
  expandedSections.value = set
}
initExpanded()

const activeMenuIndex = computed(() => {
  const currentName = (route.name as string | undefined) ?? ''
  const currentPath = route.path

  for (const topRoute of menuRoutes as any[]) {
    if (!topRoute.children || topRoute.children.length === 0) {
      if (topRoute.name === currentName || topRoute.path === currentPath) {
        return topRoute.name || topRoute.path || ''
      }
      continue
    }

    for (const child of topRoute.children) {
      if (child.meta?.isDetail) continue
      if (child.name === currentName || currentPath.startsWith(child.path)) {
        return child.name as string
      }
    }
  }
  return ''
})

const openedTopMenus = computed(() => {
  const currentPath = route.path
  const top = (menuRoutes as any[]).find(item => {
    if (!item.children || item.children.length === 0) return false
    return currentPath === item.path || currentPath.startsWith(`${item.path}/`)
  })
  return top ? [top.path || top.name || ''] : []
})

const isActiveTop = (topRoute: any) => {
  if (!topRoute.children || topRoute.children.length === 0) {
    return activeMenuIndex.value === (topRoute.name || topRoute.path || '')
  }
  return openedTopMenus.value.includes(topRoute.path || topRoute.name || '')
}

const isActiveChild = (child: any) => {
  return activeMenuIndex.value === child.name
}

const isExpanded = (topRoute: any) => {
  const key = topRoute.path || topRoute.name || ''
  return expandedSections.value.has(key)
}

const toggleSection = (topRoute: any) => {
  const key = topRoute.path || topRoute.name || ''
  const newSet = new Set(expandedSections.value)
  if (newSet.has(key)) {
    newSet.delete(key)
  } else {
    newSet.add(key)
  }
  expandedSections.value = newSet
}

const handleMenuClick = (routeName: string) => {
  router.push({ name: routeName })
}

const getIconClass = (icon?: string) => {
  const map: Record<string, string> = {
    'dashboard': 'fa-solid fa-chart-line',
    'system': 'fa-solid fa-gears',
    'monitor': 'fa-solid fa-desktop',
    'moderation': 'fa-solid fa-shield-halved',
    'content': 'fa-solid fa-newspaper',
    'users': 'fa-solid fa-users',
    'user-tag': 'fa-solid fa-user-tag',
    'key': 'fa-solid fa-key',
    'user-clock': 'fa-solid fa-user-clock',
    'heart-pulse': 'fa-solid fa-heart-pulse',
    'globe': 'fa-solid fa-globe',
    'clipboard-check': 'fa-solid fa-clipboard-check',
    'cubes': 'fa-solid fa-cubes',
    'file-lines': 'fa-solid fa-file-lines',
    'tags': 'fa-solid fa-tags',
    'folder-tree': 'fa-solid fa-folder-tree',
    'flag': 'fa-solid fa-flag',
  }
  return map[icon || ''] || 'fa-solid fa-circle'
}
</script>

<template>
  <aside :class="['sidebar', { collapsed: collapse }]">
    <div class="sidebar-header">
      <a href="#" class="sidebar-logo">
        <img src="../../assets/logo.svg" width="32" height="32" alt="WaterFun">
        <span v-show="!collapse" class="sidebar-logo-text">WaterFun</span>
      </a>
    </div>
    <nav class="sidebar-nav no-scroll-bar">
      <template v-for="route in menuRoutes" :key="route.path || route.name">
        <div v-if="route.children && route.children.length" class="nav-section">
          <div class="nav-section-header" :class="{ expanded: isExpanded(route) }" @click="toggleSection(route)">
            <div class="nav-section-title-wrap">
              <i :class="['nav-section-icon', getIconClass(route.meta?.icon)]"></i>
              <span v-show="!collapse" class="nav-section-title">{{ route.meta?.locale }}</span>
            </div>
            <i v-show="!collapse" class="nav-section-arrow fa-solid fa-chevron-right"></i>
          </div>
          <div :class="['nav-items', { expanded: isExpanded(route) }]">
            <a
              v-for="child in route.children.filter(c => !c.meta?.isDetail)"
              :key="child.name"
              :class="['nav-item', { active: isActiveChild(child) }]"
              @click="handleMenuClick(child.name as string)"
            >
              <i :class="getIconClass(child.meta?.icon)"></i>
              <span v-show="!collapse">{{ child.meta?.locale }}</span>
            </a>
          </div>
        </div>
        <a
          v-else-if="!route.meta?.isDetail"
          :key="route.name || route.path"
          :class="['nav-item', { active: isActiveTop(route) }]"
          @click="handleMenuClick(route.name as string)"
        >
          <i :class="getIconClass(route.meta?.icon)"></i>
          <span v-show="!collapse">{{ route.meta?.locale }}</span>
        </a>
      </template>
    </nav>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 240px;
  background: var(--sidebar-bg);
  color: #cbd5e1;
  display: flex;
  flex-direction: column;
  height: 100vh;
  transition: width 0.3s ease;
  overflow: hidden;
  flex-shrink: 0;
}

.sidebar.collapsed {
  width: 60px;
}

.sidebar-header {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  flex-shrink: 0;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: white;
  text-decoration: none;
}

.sidebar-logo-text {
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
}

.sidebar-nav {
  flex: 1;
  padding: 12px 10px;
  overflow-y: auto;
  overflow-x: hidden;
}

.nav-section {
  margin-bottom: 4px;
}

.nav-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all 0.2s ease;
  user-select: none;
}

.nav-section-header:hover {
  background: var(--sidebar-hover);
}

.nav-section-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.nav-section-icon {
  width: 20px;
  text-align: center;
  font-size: 14px;
  color: #64748b;
  flex-shrink: 0;
}

.nav-section-title {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  white-space: nowrap;
  transition: opacity 0.2s;
}

.sidebar.collapsed .nav-section-title,
.sidebar.collapsed .nav-section-arrow,
.sidebar.collapsed .nav-item span {
  opacity: 0;
  width: 0;
  overflow: hidden;
}

.nav-section-arrow {
  font-size: 10px;
  color: #64748b;
  transition: transform 0.2s ease;
  flex-shrink: 0;
}

.nav-section-header.expanded .nav-section-arrow {
  transform: rotate(90deg);
}

.nav-items {
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.3s ease;
}

.nav-items.expanded {
  max-height: 400px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 14px 9px 44px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: #94a3b8;
  text-decoration: none;
  transition: all 0.2s ease;
  cursor: pointer;
  border: none;
  background: transparent;
  width: 100%;
  white-space: nowrap;
}

.nav-item:hover {
  background: var(--sidebar-hover);
  color: #e2e8f0;
}

.nav-item.active {
  background: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
}

.nav-item i {
  width: 20px;
  text-align: center;
  font-size: 14px;
  flex-shrink: 0;
}

.nav-item span {
  transition: opacity 0.2s;
}
</style>
