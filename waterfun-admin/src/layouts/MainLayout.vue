<script setup lang="ts">
import MainNavBar from "@/layouts/main/MainNavBar.vue"
import { ref, computed, onBeforeMount, watch } from 'vue'
import type { BreadNavItemType, TagNavItemType } from "@/types/ui/tagNav.js"
import TagNavigation from "@/layouts/main/TagNavigation.vue"
import AsideNavBar from "@/layouts/main/AsideNavBar.vue"
import { useRouter } from "vue-router"
import { useTagStore } from "~/stores/tagStore"
import { useAuthStore } from '@/stores/authStore'

const menuCollapse = ref(false)
const tagStore = useTagStore()
const activeTags = ref<string>('dashboard')
const authStore = useAuthStore()
const router = useRouter()

const tagList = ref<TagNavItemType[]>([])
const breadList = computed(() => {
  return tagList.value.map((tag: TagNavItemType) => {
    return { name: tag.name, locale: tag.locale, to: tag.to } as BreadNavItemType
  })
})

const dashboardTagItem: TagNavItemType = {
  name: 'dashboard',
  locale: '仪表盘',
  to: 'dashboard',
  closeable: false
}

const addNavTags = (to: any) => {
  if (to.name && to.meta && to.meta.locale && to.meta.public !== true) {
    tagStore.addTag({
      name: to.name,
      to: to.name,
      params: to.params,
      closeable: true,
      locale: to.meta.locale,
    })
  }
}

const handleRemoveTag = (tagName: string) => {
  let name = null
  if (activeTags.value === tagName) {
    const currentTags = tagStore.getTags
    const currentIndex = currentTags.findIndex((t) => t.name === tagName)
    const targetTag = currentTags[currentIndex - 1] || currentTags[currentIndex + 1]
    if (targetTag) {
      name = targetTag.name
      router.push({ name: targetTag.to })
    }
  }
  tagStore.removeTag(tagName)
  if (name) activeTags.value = name
}

const handleOrderUpdated = ({ from, to }: { from: number; to: number }) => {
  const newTags = [...tagStore.getTags]
  const [moved] = newTags.splice(from, 1)
  if (moved) {
    newTags.splice(to, 0, moved)
    tagStore.updateTags(newTags)
  }
}

onBeforeMount(() => {
  tagStore.addTag(dashboardTagItem)
})

router.afterEach((to) => {
  addNavTags(to)
  activeTags.value = (to.name as string) || 'dashboard'
})

watch(() => tagStore.getTags, (newTags) => {
  tagList.value = newTags
}, { immediate: true, deep: true })
</script>

<template>
  <div class="admin-layout">
    <AsideNavBar :collapse="menuCollapse" />
    <div class="main-content">
      <MainNavBar @collapse="menuCollapse = !menuCollapse" />
      <div class="content-header default-border-bottom">
        <TagNavigation
          v-model="activeTags"
          class="tag-nav"
          :tag-list="tagList"
          @order-updated="handleOrderUpdated"
          @tag-removed="handleRemoveTag"
        />
      </div>
      <div class="content-area">
        <RouterView :key="$route.fullPath" />
      </div>
      <div class="content-footer">
        <div class="copyright">CopyRight © WaterFun 2025</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.content-header {
  background: var(--bg-white);
  flex-shrink: 0;
}

.content-area {
  padding: 24px 28px;
  flex: 1;
  overflow-y: auto;
  background: var(--bg);
}

.content-footer {
  height: 30px;
  font-size: 12px;
  background: var(--bg-white);
  border-top: 1px solid var(--border);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 16px;
}

.copyright {
  color: var(--text-muted);
}

.tag-nav {
  display: flex;
  align-items: center;
  width: 100%;
  min-width: 0;
}
</style>
