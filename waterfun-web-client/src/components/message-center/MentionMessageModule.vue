<template>
  <el-card class="module-card" shadow="never">
    <template #header>
      <div class="module-header">
        <h3>{{ $t('notification.mention') }}</h3>
        <el-button v-if="hasUnread" size="small" text type="primary" @click="handleMarkAllRead">
          {{ $t('notification.markAllRead') }}
        </el-button>
      </div>
    </template>

    <div class="list" @scroll.passive="onScroll">
      <el-card
        v-for="(item, idx) in notifications"
        :key="`${item.id}-${idx}`"
        class="notification-card"
        shadow="hover"
        :class="{ unread: !item.isRead }"
        @click="handleMarkRead(item)"
      >
        <div class="notification-header">
          <div class="title-wrap">
            <span v-if="!item.isRead" class="unread-dot" />
            <span class="notification-title">{{ item.title || '提及消息' }}</span>
          </div>
          <span class="notification-time">{{ formatTime(item.createdAt) }}</span>
        </div>
        <div class="notification-content">{{ item.content?.displayText || '' }}</div>
      </el-card>

      <el-skeleton v-if="loading && notifications.length === 0" :rows="5" animated class="skeleton" />
      <el-empty v-else-if="!loading && notifications.length === 0" :description="$t('message.empty.noNotifications', { type: '' })" />
      <div v-if="loading && notifications.length > 0" class="list-footer">{{ $t('notification.loading') }}</div>
      <div v-else-if="!hasNext && notifications.length > 0" class="list-footer">{{ $t('notification.noMore') }}</div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed, watch, ref } from "vue"
import { ElMessage } from "element-plus"
import { useNotificationStore } from "~/stores/notificationStore"

const props = defineProps<{ active: boolean }>()

const notificationStore = useNotificationStore()
const loadedOnce = ref(false)

const notifications = computed(() => notificationStore.getNotifications('mention'))
const loading = computed(() => notificationStore.isLoading('mention'))
const hasNext = computed(() => notificationStore.hasMore('mention'))
const hasUnread = computed(() => notifications.value.some((n) => !n.isRead))

const formatTime = (dateStr: string): string => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', { hour12: false })
}

const load = (reset = false) => {
  notificationStore.fetchNotifications('mention', reset)
}

const onScroll = (event: Event) => {
  const target = event.target as HTMLDivElement
  const threshold = 60
  if (target.scrollTop + target.clientHeight >= target.scrollHeight - threshold) {
    load()
  }
}

const handleMarkRead = async (item: any) => {
  if (!item.isRead) {
    await notificationStore.markAsRead(item.id)
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
.module-card { border-radius: 12px; }
.module-header { display: flex; align-items: center; justify-content: space-between; }
.module-header h3 { margin: 0; font-size: 20px; color: #111827; }
.list { height: calc(100vh - 240px); min-height: 460px; overflow-y: auto; padding-right: 8px; }
.notification-card { margin-bottom: 14px; border-radius: 10px; cursor: pointer; }
.notification-card.unread { border-left: 3px solid #409eff; }
.notification-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.title-wrap { display: inline-flex; align-items: center; gap: 8px; }
.notification-title { font-size: 16px; font-weight: 600; color: #111827; }
.unread-dot { width: 8px; height: 8px; border-radius: 50%; background: #ef4444; flex-shrink: 0; }
.notification-time { font-size: 13px; color: #6b7280; white-space: nowrap; }
.notification-content { margin-top: 10px; color: #374151; line-height: 1.7; }
.list-footer { text-align: center; color: #6b7280; padding: 14px 0 8px; font-size: 13px; }
.skeleton { margin-top: 12px; }
@media (max-width: 768px) { .list { height: auto; min-height: 360px; } }
</style>
