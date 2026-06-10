<script setup lang="ts">
import { ref } from 'vue'
import type { BreadNavItemType } from "@/types/ui/tagNav.js"
import { useBreadcrumbs } from "@/composables/useBreadcrumbs.js"
import UserMenu from "@/layouts/main/UserMenu.vue"

const emit = defineEmits(['collapse'])
const menuCollapse = ref(false)

const collapseButtonClick = () => {
  menuCollapse.value = !menuCollapse.value
  emit('collapse')
}

const bcStore = useBreadcrumbs()
</script>

<template>
  <header class="top-bar">
    <div class="top-bar-left">
      <button class="collapse-btn" @click="collapseButtonClick">
        <i :class="menuCollapse ? 'fa-solid fa-chevron-right' : 'fa-solid fa-bars'"></i>
      </button>
      <div class="breadcrumb">
        <span v-for="(item, idx) in bcStore.breadcrumbs.value" :key="idx" class="breadcrumb-item">
          <span v-if="idx > 0" class="separator">/</span>
          <span class="breadcrumb-text">{{ item.locale }}</span>
        </span>
      </div>
    </div>
    <div class="top-bar-right">
      <button class="top-btn"><i class="fa-regular fa-bell"></i></button>
      <UserMenu />
    </div>
  </header>
</template>

<style scoped>
.top-bar {
  height: 60px;
  background: var(--bg-white);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  position: sticky;
  top: 0;
  z-index: 50;
  flex-shrink: 0;
}

.top-bar-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  min-width: 0;
}

.collapse-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: all 0.2s;
}

.collapse-btn:hover {
  background: var(--bg);
  color: var(--primary);
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
}

.breadcrumb-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.separator {
  color: var(--text-muted);
  margin-right: 4px;
}

.breadcrumb-text {
  color: var(--text-secondary);
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.top-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  position: relative;
  transition: all 0.2s;
}

.top-btn:hover {
  background: var(--bg);
  color: var(--primary);
}

.top-btn .badge {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 8px;
  height: 8px;
  background: var(--danger);
  border-radius: 50%;
  border: 2px solid var(--bg-white);
}
</style>
