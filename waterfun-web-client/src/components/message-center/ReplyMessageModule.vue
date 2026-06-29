<template>
  <el-card shadow="never">
    <template #header>
      <div class="wf-flex-between">
        <h3 class="wf-module-header">{{ $t('notification.reply') }}</h3>
        <el-button v-if="hasUnread" size="small" text type="primary" @click="handleMarkAllRead">
          {{ $t('notification.markAllRead') }}
        </el-button>
      </div>
    </template>

    <div class="list" @scroll.passive="onScroll">
      <el-card
        v-for="(item, idx) in notifications"
        :key="`${item.id}-${idx}`"
        class="wf-notification-card"
        shadow="hover"
        :class="{ unread: !item.isRead }"
        @click="handleMarkRead(item)"
      >
        <div class="wf-notification-body">
          <div class="wf-notification-text">
            <div class="wf-notification-header">
              <div class="wf-title-wrap">
                <span v-if="!item.isRead" class="wf-unread-dot" />
                <span class="wf-notification-title">{{ item.title || '回复消息' }}</span>
              </div>
              <span class="wf-notification-time">{{ formatTime(item.createdAt) }}</span>
            </div>
            <div class="wf-notification-content">{{ getNotificationText(item) }}</div>
          </div>
          <img v-if="item.content?.postCoverage?.url" :src="item.content.postCoverage.url" class="wf-cover-thumb" />
        </div>
      </el-card>

      <el-skeleton v-if="loading && notifications.length === 0" :rows="5" animated class="skeleton" />
      <el-empty v-else-if="!loading && notifications.length === 0" :description="$t('message.empty.noNotifications', { type: '' })" />
      <div v-if="loading && notifications.length > 0" class="wf-list-footer">{{ $t('notification.loading') }}</div>
      <div v-else-if="!hasNext && notifications.length > 0" class="wf-list-footer">{{ $t('notification.noMore') }}</div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed, watch, ref } from "vue"
import { ElMessage } from "element-plus"
import { useRouter } from "vue-router"
import { getNotificationText } from "~/api/notificationApi"
import { useNotificationStore } from "~/stores/notificationStore"

const props = defineProps<{ active: boolean }>()

const notificationStore = useNotificationStore()
const loadedOnce = ref(false)

const notifications = computed(() => notificationStore.getNotifications('reply'))
const loading = computed(() => notificationStore.isLoading('reply'))
const hasNext = computed(() => notificationStore.hasMore('reply'))
const hasUnread = computed(() => notifications.value.some((n) => !n.isRead))

const formatTime = (dateStr: string): string => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', { hour12: false })
}

const load = (reset = false) => {
  notificationStore.fetchNotifications('reply', reset)
}

const onScroll = (event: Event) => {
  const target = event.target as HTMLDivElement
  const threshold = 60
  if (target.scrollTop + target.clientHeight >= target.scrollHeight - threshold) {
    load()
  }
}

const router = useRouter()

const handleMarkRead = async (item: any) => {
  if (!item.isRead) {
    await notificationStore.markAsRead(item.id)
  }
  if (item.content?.nativeUrl) {
    router.push(item.content.nativeUrl)
  }
}

const handleMarkAllRead = async () => {
  await notificationStore.markAllRead()
  ElMessage.success('已全部标记为已读')
}

watch(
  () => props.active,
  (active) => {
    if (active && !loadedOnce.value) {
      loadedOnce.value = true
      load(true)
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.list { height: calc(100vh - 240px); min-height: 460px; overflow-y: auto; padding-right: 8px; }
.wf-notification-body { display: flex; flex-direction: row; gap: 12px; align-items: flex-start; }
.wf-notification-text { flex: 1; min-width: 0; }
.wf-cover-thumb { width: 64px; height: 64px; border-radius: 6px; object-fit: cover; flex-shrink: 0; }
.skeleton { margin-top: 12px; }
@media (max-width: 768px) { .list { height: auto; min-height: 360px; } }
</style>
