<script setup lang="ts">
import MainNavBar from "@/layouts/main/MainNavBar.vue"
import { ref, computed, onBeforeMount, watch, nextTick, onUnmounted } from 'vue'
import type { BreadNavItemType, TagNavItemType } from "@/types/ui/tagNav.js"
import TagNavigation from "@/layouts/main/TagNavigation.vue"
import AsideNavBar from "@/layouts/main/AsideNavBar.vue"
import { useRouter } from "vue-router"
import { useTagStore } from "~/stores/tagStore"
import { useAuthStore } from '@/stores/authStore'

const menuCollapse = ref(false)
const mobileMenuOpen = ref(false)
const isMobile = ref(false)
const tagStore = useTagStore()
const activeTags = ref<string>('dashboard')
const authStore = useAuthStore()
const router = useRouter()
let isMounted = false
let mediaQuery: MediaQueryList | null = null
const onMediaChange = (e: MediaQueryListEvent) => {
  isMobile.value = e.matches
  if (!e.matches) mobileMenuOpen.value = false
}

const handleMobileToggle = () => {
  mobileMenuOpen.value = !mobileMenuOpen.value
}

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
  mediaQuery = window.matchMedia('(max-width: 768px)')
  isMobile.value = mediaQuery.matches
})

onMounted(() => {
  isMounted = true
  mediaQuery = window.matchMedia('(max-width: 768px)')
  isMobile.value = mediaQuery.matches
  mediaQuery.addEventListener('change', onMediaChange)
})

onUnmounted(() => {
  isMounted = false
  if (mediaQuery) {
    mediaQuery.removeEventListener('change', onMediaChange)
  }
})

// Defer tag updates to after component lifecycle settles
// to avoid modifying reactive state during component teardown/creation
let afterEachCleanup: (() => void) | null = null
router.isReady().then(() => {
  afterEachCleanup = router.afterEach((to) => {
    nextTick(() => {
      if (!isMounted) return
      addNavTags(to)
      activeTags.value = (to.name as string) || 'dashboard'
    })
  })
})

onUnmounted(() => {
  if (afterEachCleanup) {
    afterEachCleanup()
    afterEachCleanup = null
  }
})

watch(() => tagStore.getTags, (newTags) => {
  if (!isMounted) return
  tagList.value = newTags
}, { immediate: true, deep: true })
</script>

<template>
  <div class="admin-layout">
    <AsideNavBar :collapse="menuCollapse" :drawer-open="mobileMenuOpen" :mobile="isMobile" />
    <div class="main-content">
      <MainNavBar :mobile="isMobile" @collapse="menuCollapse = !menuCollapse" @toggle-mobile="handleMobileToggle" />
      <div v-if="isMobile && mobileMenuOpen" class="mobile-backdrop" @click="mobileMenuOpen = false" />
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
        <RouterView />
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

.mobile-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  z-index: 1000;
}
</style>
