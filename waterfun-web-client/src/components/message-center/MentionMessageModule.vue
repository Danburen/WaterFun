<template>
  <el-card shadow="never">
    <template #header>
      <div class="wf-flex-between">
        <h3 class="wf-module-header">{{ $t('notification.mention') }}</h3>
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
        <div class="wf-notification-header">
          <div class="wf-title-wrap">
            <span v-if="!item.isRead" class="wf-unread-dot" />
            <span class="wf-notification-title">{{ item.title || '提及消息' }}</span>
          </div>
          <span class="wf-notification-time">{{ formatTime(item.createdAt) }}</span>
        </div>
        <div class="wf-notification-content">{{ item.content?.displayText || '' }}</div>
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
.list { height: calc(100vh - 240px); min-height: 460px; overflow-y: auto; padding-right: 8px; }
.skeleton { margin-top: 12px; }
@media (max-width: 768px) { .list { height: auto; min-height: 360px; } }
</style>
